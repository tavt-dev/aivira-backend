package com.tien.aivirabackend.service.ai;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.config.properties.OpenAiProperties;
import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.dto.request.*;
import com.tien.aivirabackend.domain.dto.response.*;
import com.tien.aivirabackend.domain.entity.ai.*;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.review.Review;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AiAdviceErrorCode;
import com.tien.aivirabackend.repository.*;
import com.tien.aivirabackend.service.auth.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAdviceService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final CurrentUserService currentUserService;
    private final AiAdviceSessionRepository sessionRepository;
    private final AiAdviceMessageRepository messageRepository;
    private final AiAdviceSnapshotRepository snapshotRepository;
    private final AiAdviceRecommendationRepository recommendationRepository;
    private final AiAdviceEventRepository eventRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;
    private final OpenAiAdvisorClient openAiClient;
    private final AiAdviceQuotaService quotaService;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiAdviceSessionResponse createSession(AiAdviceSessionCreateRequest request) {
        User user = currentUserService.getCurrentUser();
        Instant now = Instant.now();
        AiAdviceSession session = AiAdviceSession.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .locale(normalizeLocale(request.locale()))
                .personalizationEnabled(request.personalizationEnabled() == null || request.personalizationEnabled())
                .lastActivityAt(now)
                .expiresAt(now.plusSeconds(properties.retentionDays() * 86400L))
                .build();
        sessionRepository.save(session);
        return toSessionResponse(session, List.of());
    }

    @Transactional(readOnly = true)
    public AiAdviceSessionResponse getSession(String sessionId) {
        AiAdviceSession session = requireSession(sessionId);
        List<AiAdviceMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(session, messages);
    }

    @Transactional
    public AiAdviceSessionResponse updatePreferences(String sessionId, AiAdvicePreferencesRequest request) {
        AiAdviceSession session = requireSession(sessionId);
        session.setPersonalizationEnabled(request.personalizationEnabled());
        touch(session);
        return toSessionResponse(session, messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    @Transactional
    public AiAdviceMessageResponse sendMessage(String sessionId, AiAdviceMessageRequest request) {
        AiAdviceSession session = requireSession(sessionId);
        Optional<AiAdviceMessage> duplicate = messageRepository.findBySessionIdAndClientMessageId(
                sessionId, request.clientMessageId());
        if (duplicate.isPresent()) {
            AiAdviceMessage assistant = messageRepository
                    .findFirstBySessionIdAndRoleAndIdGreaterThanOrderByIdAsc(
                            sessionId, AiAdviceRole.ASSISTANT, duplicate.get().getId())
                    .orElseThrow(() -> new AppException(AiAdviceErrorCode.REQUEST_IN_PROGRESS));
            return toMessageResponse(assistant, true);
        }

        String userId = session.getUser().getId();
        Long usageId = quotaService.reserve(userId, request.clientMessageId());
        try {
            List<AiAdviceMessage> recent = messageRepository.findBySessionIdOrderByCreatedAtDesc(
                    sessionId, PageRequest.of(0, 12));
            Collections.reverse(recent);
            List<AiConversationTurn> history = recent.stream()
                    .map(message -> new AiConversationTurn(
                            message.getRole() == AiAdviceRole.USER ? "user" : "assistant", message.getContent()))
                    .collect(Collectors.toCollection(ArrayList::new));
            history.add(new AiConversationTurn("user", request.content().trim()));

            AiModelResult<AiSearchProfile> analyzed = openAiClient.analyze(
                    history,
                    session.isPersonalizationEnabled() ? personalizationContext(userId) : null,
                    session.getLocale(),
                    safetyIdentifier(userId));
            AiSearchProfile profile = analyzed.value();

            AiAdviceMessage userMessage = messageRepository.save(AiAdviceMessage.builder()
                    .session(session)
                    .role(AiAdviceRole.USER)
                    .content(request.content().trim())
                    .clientMessageId(request.clientMessageId())
                    .build());

            AiAdviceMessage assistant;
            if (profile.needsClarification()) {
                assistant = saveAssistant(
                        session,
                        profile.clarificationQuestion(),
                        AiAdviceResponseStatus.CLARIFICATION,
                        analyzed,
                        List.of());
            } else {
                List<Product> ranked = rankProducts(profile, session.isPersonalizationEnabled() ? purchasedIds(userId) : Set.of());
                if (ranked.isEmpty()) {
                    String text = session.getLocale().startsWith("en")
                            ? "I could not find an available book matching those preferences. Try broadening the topic or budget."
                            : "Mình chưa tìm thấy sách đang bán phù hợp. Bạn thử mở rộng chủ đề hoặc khoảng giá nhé.";
                    assistant = saveAssistant(
                            session, text, AiAdviceResponseStatus.NO_RESULTS, analyzed, List.of());
                } else {
                    List<Product> firstPage = ranked.subList(0, Math.min(properties.pageSize(), ranked.size()));
                    AiModelResult<AiAdviceDraft> explained = openAiClient.explain(
                            profile, toCandidates(firstPage), session.getLocale(), safetyIdentifier(userId));
                    assistant = saveAssistant(
                            session,
                            explained.value().message(),
                            AiAdviceResponseStatus.RECOMMENDATION,
                            combine(analyzed, explained),
                            explained.value().suggestedPrompts());
                    persistSnapshot(assistant, profile, ranked, explained.value());
                }
            }

            touch(session);
            messageRepository.flush();
            quotaService.complete(usageId);
            return toMessageResponse(assistant, true);
        } catch (RuntimeException ex) {
            quotaService.fail(usageId);
            throw ex;
        }
    }

    @Transactional
    public AiAdviceRecommendationPageResponse getRecommendations(String sessionId, Long messageId, int page) {
        AiAdviceSession session = requireSession(sessionId);
        AiAdviceResultSnapshot snapshot = snapshotRepository
                .findByMessageIdAndMessageSessionId(messageId, sessionId)
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.MESSAGE_NOT_FOUND));
        int normalizedPage = Math.max(1, page);
        List<AiAdviceRecommendation> rows = recommendationRepository.findBySnapshotIdOrderByRankPositionAsc(
                snapshot.getId(), PageRequest.of(normalizedPage - 1, properties.pageSize()));
        if (rows.stream().anyMatch(row -> !StringUtils.hasText(row.getReason()))) {
            enrichReasons(session, snapshot, rows);
        }
        recordImpressions(session, snapshot.getMessage(), rows);
        return toPage(snapshot, rows, normalizedPage);
    }

    @Transactional
    public void recordEvent(String sessionId, AiAdviceEventRequest request) {
        AiAdviceSession session = requireSession(sessionId);
        if (request.eventType() == AiAdviceEventType.IMPRESSION) {
            throw new AppException(AiAdviceErrorCode.INVALID_EVENT);
        }
        if (request.eventType() == AiAdviceEventType.CLICK) {
            if (request.recommendationId() == null) throw new AppException(AiAdviceErrorCode.INVALID_EVENT);
            AiAdviceRecommendation recommendation = recommendationRepository
                    .findByIdAndSnapshotMessageSessionId(request.recommendationId(), sessionId)
                    .orElseThrow(() -> new AppException(AiAdviceErrorCode.RECOMMENDATION_NOT_FOUND));
            eventRepository.save(AiAdviceEvent.builder()
                    .session(session)
                    .message(recommendation.getSnapshot().getMessage())
                    .recommendation(recommendation)
                    .eventType(AiAdviceEventType.CLICK)
                    .build());
            return;
        }
        if (request.messageId() == null
                || (request.eventType() != AiAdviceEventType.HELPFUL
                        && request.eventType() != AiAdviceEventType.NOT_HELPFUL)) {
            throw new AppException(AiAdviceErrorCode.INVALID_EVENT);
        }
        AiAdviceMessage message = messageRepository
                .findByIdAndSessionId(request.messageId(), sessionId)
                .filter(value -> value.getRole() == AiAdviceRole.ASSISTANT)
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.MESSAGE_NOT_FOUND));
        AiAdviceEvent feedback = eventRepository
                .findBySessionIdAndMessageIdAndEventTypeIn(
                        sessionId,
                        message.getId(),
                        List.of(AiAdviceEventType.HELPFUL, AiAdviceEventType.NOT_HELPFUL))
                .orElseGet(() -> AiAdviceEvent.builder().session(session).message(message).build());
        feedback.setEventType(request.eventType());
        eventRepository.save(feedback);
    }

    public AiAdviceQuotaResponse getQuota() {
        return quotaService.getQuota(currentUserService.getCurrentUserId());
    }

    @Scheduled(cron = "${openai.cleanup-cron:0 25 3 * * *}")
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(Instant.now());
        quotaService.releaseStaleReservations(Instant.now().minusSeconds(600));
    }

    private AiAdviceSession requireSession(String sessionId) {
        return sessionRepository
                .findByIdAndUserId(sessionId, currentUserService.getCurrentUserId())
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.SESSION_NOT_FOUND));
    }

    private AiAdviceMessage saveAssistant(
            AiAdviceSession session,
            String content,
            AiAdviceResponseStatus status,
            AiModelResult<?> result,
            List<String> suggestedPrompts) {
        return messageRepository.save(AiAdviceMessage.builder()
                .session(session)
                .role(AiAdviceRole.ASSISTANT)
                .content(content)
                .responseStatus(status)
                .model(result.model())
                .inputTokens(result.inputTokens())
                .outputTokens(result.outputTokens())
                .latencyMs(result.latencyMs())
                .suggestedPrompts(writeJson(suggestedPrompts == null ? List.of() : suggestedPrompts))
                .build());
    }

    private void persistSnapshot(
            AiAdviceMessage assistant, AiSearchProfile profile, List<Product> products, AiAdviceDraft draft) {
        AiAdviceResultSnapshot snapshot = snapshotRepository.save(AiAdviceResultSnapshot.builder()
                .message(assistant)
                .searchProfile(writeJson(profile))
                .totalResults(products.size())
                .build());
        assistant.setSnapshot(snapshot);

        Map<Long, AiAdviceDraft.BookReason> reasonByProduct = draft.recommendations().stream()
                .filter(reason -> reason.productId() != null)
                .collect(Collectors.toMap(AiAdviceDraft.BookReason::productId, Function.identity(), (left, right) -> left));
        List<AiAdviceRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            AiAdviceDraft.BookReason reason = reasonByProduct.get(product.getId());
            recommendations.add(AiAdviceRecommendation.builder()
                    .snapshot(snapshot)
                    .product(product)
                    .rankPosition(i + 1)
                    .reason(reason == null ? null : reason.reason())
                    .matchedCriteria(reason == null ? null : writeJson(reason.matchedCriteria()))
                    .build());
        }
        recommendationRepository.saveAll(recommendations);
        snapshot.setRecommendations(recommendations);
    }

    private void enrichReasons(
            AiAdviceSession session, AiAdviceResultSnapshot snapshot, List<AiAdviceRecommendation> rows) {
        try {
            AiSearchProfile profile = objectMapper.readValue(snapshot.getSearchProfile(), AiSearchProfile.class);
            AiModelResult<AiAdviceDraft> explained = openAiClient.explain(
                    profile,
                    toCandidates(rows.stream().map(AiAdviceRecommendation::getProduct).toList()),
                    session.getLocale(),
                    safetyIdentifier(session.getUser().getId()));
            Map<Long, AiAdviceDraft.BookReason> reasons = explained.value().recommendations().stream()
                    .collect(Collectors.toMap(AiAdviceDraft.BookReason::productId, Function.identity(), (a, b) -> a));
            for (AiAdviceRecommendation row : rows) {
                AiAdviceDraft.BookReason reason = reasons.get(row.getProduct().getId());
                if (reason != null) {
                    row.setReason(reason.reason());
                    row.setMatchedCriteria(writeJson(reason.matchedCriteria()));
                }
            }
        } catch (AppException | JacksonException ignored) {
            for (AiAdviceRecommendation row : rows) {
                if (!StringUtils.hasText(row.getReason())) {
                    row.setReason(session.getLocale().startsWith("en")
                            ? "This book matches the preferences identified in your consultation."
                            : "Cuốn sách này phù hợp với các sở thích đã xác định trong phiên tư vấn.");
                    row.setMatchedCriteria("[]");
                }
            }
        }
    }

    private List<Product> rankProducts(AiSearchProfile profile, Set<Long> purchased) {
        return productRepository.findByActiveTrueAndStatusAndStockQuantityGreaterThan(ProductStatus.ACTIVE, 0).stream()
                .filter(product -> profile.minPrice() == null || product.getPrice().compareTo(profile.minPrice()) >= 0)
                .filter(product -> profile.maxPrice() == null || product.getPrice().compareTo(profile.maxPrice()) <= 0)
                .filter(product -> profile.languages() == null
                        || profile.languages().isEmpty()
                        || containsAny(product.getBookLanguage(), profile.languages()))
                .sorted(Comparator.comparingDouble((Product product) -> score(product, profile, purchased))
                        .reversed()
                        .thenComparing(Product::getId))
                .toList();
    }

    private double score(Product product, AiSearchProfile profile, Set<Long> purchased) {
        String haystack = String.join(
                        " ",
                        safe(product.getProductName()),
                        safe(product.getBookAuthor()),
                        safe(product.getDescription()),
                        product.getCategory() == null ? "" : safe(product.getCategory().getCategoryName()))
                .toLowerCase(Locale.ROOT);
        double score = 0;
        score += matchCount(haystack, profile.searchTerms()) * 12;
        score += matchCount(haystack, profile.categoryHints()) * 10;
        score += matchCount(haystack, profile.authorHints()) * 12;
        score += product.getAverageRating() == null ? 0 : product.getAverageRating().doubleValue() * 2;
        score += Math.log1p(Math.max(0, product.getSoldCount())) * 2;
        if (purchased.contains(product.getId())) score -= 25;
        return score;
    }

    private int matchCount(String haystack, List<String> values) {
        if (values == null) return 0;
        return (int) values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(haystack::contains)
                .count();
    }

    private boolean containsAny(String value, List<String> expected) {
        if (!StringUtils.hasText(value)) return false;
        String normalized = value.toLowerCase(Locale.ROOT);
        return expected.stream()
                .filter(StringUtils::hasText)
                .anyMatch(item -> normalized.contains(item.toLowerCase(Locale.ROOT)));
    }

    private String personalizationContext(String userId) {
        List<Order> orders = orderRepository.findTop10ByUserIdAndOrderStatusOrderByCreatedAtDesc(
                userId, OrderStatus.COMPLETED);
        List<Long> purchased = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::getProductId)
                .distinct()
                .limit(30)
                .toList();
        List<Review> reviews = reviewRepository.findTop20ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        String reviewed = reviews.stream()
                .map(review -> review.getProduct().getProductName() + "=" + review.getRating() + "/5")
                .collect(Collectors.joining(", "));
        return "Previously purchased product IDs: " + purchased + "; recent ratings: " + reviewed;
    }

    private Set<Long> purchasedIds(String userId) {
        return orderRepository.findTop10ByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, OrderStatus.COMPLETED).stream()
                .flatMap(order -> order.getItems().stream())
                .map(OrderItem::getProductId)
                .collect(Collectors.toSet());
    }

    private List<AiBookCandidate> toCandidates(List<Product> products) {
        return products.stream()
                .map(product -> new AiBookCandidate(
                        product.getId(),
                        product.getProductName(),
                        product.getBookAuthor(),
                        product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                        truncate(product.getDescription(), 600),
                        product.getBookLanguage(),
                        product.getPrice(),
                        product.getAverageRating(),
                        product.getSoldCount()))
                .toList();
    }

    private void recordImpressions(
            AiAdviceSession session, AiAdviceMessage message, List<AiAdviceRecommendation> rows) {
        for (AiAdviceRecommendation row : rows) {
            if (!eventRepository.existsBySessionIdAndRecommendationIdAndEventType(
                    session.getId(), row.getId(), AiAdviceEventType.IMPRESSION)) {
                eventRepository.save(AiAdviceEvent.builder()
                        .session(session)
                        .message(message)
                        .recommendation(row)
                        .eventType(AiAdviceEventType.IMPRESSION)
                        .build());
            }
        }
    }

    private AiAdviceSessionResponse toSessionResponse(AiAdviceSession session, List<AiAdviceMessage> messages) {
        List<AiAdviceMessageResponse> responses = messages.stream()
                .map(message -> toMessageResponse(message, false))
                .toList();
        return new AiAdviceSessionResponse(
                session.getId(),
                session.getLocale(),
                session.isPersonalizationEnabled(),
                session.getExpiresAt(),
                responses,
                quotaService.getQuota(session.getUser().getId()));
    }

    private AiAdviceMessageResponse toMessageResponse(AiAdviceMessage message, boolean includeQuota) {
        AiAdviceRecommendationPageResponse page = null;
        if (message.getSnapshot() != null) {
            List<AiAdviceRecommendation> rows = message.getSnapshot().getRecommendations().stream()
                    .sorted(Comparator.comparingInt(AiAdviceRecommendation::getRankPosition))
                    .limit(properties.pageSize())
                    .toList();
            page = toPage(message.getSnapshot(), rows, 1);
            if (includeQuota) {
                recordImpressions(message.getSession(), message, rows);
            }
        }
        return new AiAdviceMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getResponseStatus(),
                message.getCreatedAt(),
                readStringList(message.getSuggestedPrompts()),
                page,
                includeQuota ? quotaService.getQuota(message.getSession().getUser().getId()) : null);
    }

    private AiAdviceRecommendationPageResponse toPage(
            AiAdviceResultSnapshot snapshot, List<AiAdviceRecommendation> rows, int page) {
        List<AiAdviceRecommendationResponse> items = rows.stream()
                .map(row -> new AiAdviceRecommendationResponse(
                        row.getId(),
                        row.getRankPosition(),
                        productMapper.toResponse(row.getProduct()),
                        row.getReason(),
                        readStringList(row.getMatchedCriteria())))
                .toList();
        int pages = (int) Math.ceil(snapshot.getTotalResults() / (double) properties.pageSize());
        return new AiAdviceRecommendationPageResponse(
                items,
                page,
                properties.pageSize(),
                snapshot.getTotalResults(),
                pages,
                page < pages);
    }

    private AiModelResult<Object> combine(AiModelResult<?> first, AiModelResult<?> second) {
        return new AiModelResult<>(
                null,
                second.model(),
                first.inputTokens() + second.inputTokens(),
                first.outputTokens() + second.outputTokens(),
                first.latencyMs() + second.latencyMs());
    }

    private void touch(AiAdviceSession session) {
        Instant now = Instant.now();
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plusSeconds(properties.retentionDays() * 86400L));
    }

    private String normalizeLocale(String locale) {
        return StringUtils.hasText(locale) && locale.startsWith("en") ? "en" : "vi";
    }

    private String safetyIdentifier(String userId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(userId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE, ex);
        }
    }

    private List<String> readStringList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JacksonException ex) {
            return List.of();
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
