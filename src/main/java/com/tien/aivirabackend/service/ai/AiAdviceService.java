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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.config.properties.AiAdviceProperties;
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
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final CurrentUserService currentUserService;
    private final AiAdviceSessionRepository sessionRepository;
    private final AiAdviceMessageRepository messageRepository;
    private final AiAdviceSnapshotRepository snapshotRepository;
    private final AiAdviceRecommendationRepository recommendationRepository;
    private final AiAdviceEventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;
    private final AiAdvisorClient advisorClient;
    private final AiAdviceQuotaService quotaService;
    private final AiCatalogRanker catalogRanker;
    private final AiAdviceProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public AiAdviceSessionResponse createSession(AiAdviceSessionCreateRequest request, String guestId) {
        Optional<String> currentUserId = currentUserService.findCurrentUserId();
        User user = currentUserId.isPresent() ? currentUserService.getCurrentUser() : null;
        String guestKey = user == null ? requireGuestKey(guestId) : null;
        Instant now = Instant.now();
        AiAdviceSession session = AiAdviceSession.builder().id(UUID.randomUUID().toString()).user(user)
                .guestKey(guestKey).locale(normalizeLocale(request.locale()))
                .personalizationEnabled(
                        user != null && (request.personalizationEnabled() == null || request.personalizationEnabled()))
                .lastActivityAt(now).expiresAt(now.plusSeconds(properties.retentionDays() * 86400L)).build();
        sessionRepository.save(session);
        return toSessionResponse(session, List.of());
    }

    @Transactional(readOnly = true)
    public AiAdviceSessionResponse getSession(String sessionId, String guestId) {
        AiAdviceSession session = requireSession(sessionId, guestId);
        List<AiAdviceMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return toSessionResponse(session, messages);
    }

    @Transactional
    public AiAdviceSessionResponse updatePreferences(String sessionId, AiAdvicePreferencesRequest request,
            String guestId) {
        AiAdviceSession session = requireSession(sessionId, guestId);
        session.setPersonalizationEnabled(session.getUser() != null && request.personalizationEnabled());
        touch(session);
        return toSessionResponse(session, messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    public AiAdviceMessageResponse sendMessage(String sessionId, AiAdviceMessageRequest request, String guestId) {
        AiAdviceSession session = requireSession(sessionId, guestId);
        Optional<AiAdviceMessage> duplicate = messageRepository.findBySessionIdAndClientMessageId(sessionId,
                request.clientMessageId());
        if (duplicate.isPresent()) {
            AiAdviceMessage assistant = messageRepository
                    .findFirstBySessionIdAndRoleAndIdGreaterThanOrderByIdAsc(sessionId, AiAdviceRole.ASSISTANT,
                            duplicate.get().getId())
                    .orElseThrow(() -> new AppException(AiAdviceErrorCode.REQUEST_IN_PROGRESS));
            return toMessageResponse(assistant, true);
        }

        String userId = session.getUser() == null ? null : session.getUser().getId();
        String actorId = userId == null ? "guest:" + session.getGuestKey() : userId;
        Long usageId = userId == null ? null : quotaService.reserve(userId, request.clientMessageId());
        try {
            List<AiAdviceMessage> recent = messageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId,
                    PageRequest.of(0, 12));
            Collections.reverse(recent);
            List<AiConversationTurn> history = recent.stream()
                    .map(message -> new AiConversationTurn(
                            message.getRole() == AiAdviceRole.USER ? "user" : "assistant", message.getContent()))
                    .collect(Collectors.toCollection(ArrayList::new));
            history.add(new AiConversationTurn("user", request.content().trim()));

            AiModelResult<AiSearchProfile> analyzed = advisorClient.analyze(history,
                    session.isPersonalizationEnabled() ? personalizationContext(userId) : null, session.getLocale(),
                    safetyIdentifier(actorId));
            AiSearchProfile profile = analyzed.value();

            if (profile.needsClarification()) {
                return finalizeRequest(sessionId, request, usageId, profile.clarificationQuestion(),
                        AiAdviceResponseStatus.CLARIFICATION, analyzed, List.of(), profile, List.of(), null, false);
            }
            List<Product> ranked = catalogRanker.rank(profile,
                    session.isPersonalizationEnabled() ? purchasedIds(userId) : Set.of());
            if (ranked.isEmpty()) {
                String text = session.getLocale().startsWith("en")
                        ? "I could not find an available book matching those preferences. Try broadening the topic or budget."
                        : "Mình chưa tìm thấy sách đang bán phù hợp. Bạn thử mở rộng chủ đề hoặc khoảng giá nhé.";
                return finalizeRequest(sessionId, request, usageId, text, AiAdviceResponseStatus.NO_RESULTS, analyzed,
                        List.of(), profile, List.of(), null, false);
            }
            AiModelResult<AiAdviceDraft> explained = advisorClient.explain(profile, toCandidates(ranked),
                    session.getLocale(), safetyIdentifier(actorId));
            return finalizeRequest(sessionId, request, usageId, explained.value().message(),
                    AiAdviceResponseStatus.RECOMMENDATION, combine(analyzed, explained),
                    explained.value().suggestedPrompts(), profile, ranked, explained.value(), true);
        } catch (RuntimeException ex) {
            return degradedResponse(session, request, usageId, ex);
        }
    }

    private AiAdviceMessageResponse finalizeRequest(String sessionId, AiAdviceMessageRequest request, Long usageId,
            String content, AiAdviceResponseStatus responseStatus, AiModelResult<?> result,
            List<String> suggestedPrompts, AiSearchProfile profile, List<Product> products, AiAdviceDraft draft,
            boolean successfulUsage) {
        return transactionTemplate.execute(transactionStatus -> {
            AiAdviceSession managedSession = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new AppException(AiAdviceErrorCode.SESSION_NOT_FOUND));
            messageRepository.save(AiAdviceMessage.builder().session(managedSession).role(AiAdviceRole.USER)
                    .content(request.content().trim()).clientMessageId(request.clientMessageId()).build());
            AiAdviceMessage assistant = saveAssistant(managedSession, content, responseStatus, result,
                    suggestedPrompts);
            if (draft != null && !products.isEmpty()) {
                persistSnapshot(assistant, profile, products, draft);
            }
            touch(managedSession);
            if (usageId != null) {
                if (successfulUsage)
                    quotaService.complete(usageId);
                else
                    quotaService.fail(usageId);
            }
            messageRepository.flush();
            return toMessageResponse(assistant, true);
        });
    }

    private AiAdviceMessageResponse degradedResponse(AiAdviceSession session, AiAdviceMessageRequest request,
            Long usageId, RuntimeException failure) {
        AiSearchProfile fallbackProfile = catalogRanker.fallbackProfile(request.content());
        List<Product> products = catalogRanker.rank(fallbackProfile, Set.of()).stream().limit(properties.pageSize())
                .toList();
        List<AiAdviceDraft.BookReason> reasons = products.stream()
                .map(product -> new AiAdviceDraft.BookReason(product.getId(),
                        session.getLocale().startsWith("en")
                                ? "This available book matches your request based on catalog information."
                                : "Sách đang còn hàng và phù hợp với tiêu chí tìm kiếm trong danh mục.",
                        catalogRanker.matchedCriteria(product, fallbackProfile)))
                .toList();
        String message = session.getLocale().startsWith("en")
                ? "AI is temporarily limited, so these suggestions use catalog ranking."
                : "AI đang tạm giới hạn nên các gợi ý này được xếp hạng trực tiếp từ danh mục.";
        AiAdviceDraft draft = products.isEmpty() ? null : new AiAdviceDraft(message, reasons, List.of());
        AiModelResult<Object> fallbackResult = new AiModelResult<>(null, "deterministic", "catalog-ranking", 0, 0, 0);
        AiAdviceMessageResponse response = finalizeRequest(session.getId(), request, usageId, message,
                AiAdviceResponseStatus.DEGRADED_RECOMMENDATION, fallbackResult, List.of(), fallbackProfile, products,
                draft, false);
        return response;
    }

    @Transactional
    public AiAdviceRecommendationPageResponse getRecommendations(String sessionId, Long messageId, int page,
            String guestId) {
        AiAdviceSession session = requireSession(sessionId, guestId);
        AiAdviceResultSnapshot snapshot = snapshotRepository.findByMessageIdAndMessageSessionId(messageId, sessionId)
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.MESSAGE_NOT_FOUND));
        int normalizedPage = Math.max(1, page);
        List<AiAdviceRecommendation> rows = recommendationRepository.findBySnapshotIdOrderByRankPositionAsc(
                snapshot.getId(), PageRequest.of(normalizedPage - 1, properties.pageSize()));
        recordImpressions(session, snapshot.getMessage(), rows);
        return toPage(snapshot, rows, normalizedPage);
    }

    @Transactional
    public void recordEvent(String sessionId, AiAdviceEventRequest request, String guestId) {
        AiAdviceSession session = requireSession(sessionId, guestId);
        if (request.eventType() == AiAdviceEventType.IMPRESSION) {
            throw new AppException(AiAdviceErrorCode.INVALID_EVENT);
        }
        if (request.eventType() == AiAdviceEventType.CLICK) {
            if (request.recommendationId() == null)
                throw new AppException(AiAdviceErrorCode.INVALID_EVENT);
            AiAdviceRecommendation recommendation = recommendationRepository
                    .findByIdAndSnapshotMessageSessionId(request.recommendationId(), sessionId)
                    .orElseThrow(() -> new AppException(AiAdviceErrorCode.RECOMMENDATION_NOT_FOUND));
            eventRepository
                    .save(AiAdviceEvent.builder().session(session).message(recommendation.getSnapshot().getMessage())
                            .recommendation(recommendation).eventType(AiAdviceEventType.CLICK).build());
            return;
        }
        if (request.messageId() == null || (request.eventType() != AiAdviceEventType.HELPFUL
                && request.eventType() != AiAdviceEventType.NOT_HELPFUL)) {
            throw new AppException(AiAdviceErrorCode.INVALID_EVENT);
        }
        AiAdviceMessage message = messageRepository.findByIdAndSessionId(request.messageId(), sessionId)
                .filter(value -> value.getRole() == AiAdviceRole.ASSISTANT)
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.MESSAGE_NOT_FOUND));
        AiAdviceEvent feedback = eventRepository
                .findBySessionIdAndMessageIdAndEventTypeIn(sessionId, message.getId(),
                        List.of(AiAdviceEventType.HELPFUL, AiAdviceEventType.NOT_HELPFUL))
                .orElseGet(() -> AiAdviceEvent.builder().session(session).message(message).build());
        feedback.setEventType(request.eventType());
        eventRepository.save(feedback);
    }

    public AiAdviceQuotaResponse getQuota() {
        return quotaService.getQuota(currentUserService.getCurrentUserId());
    }

    @Scheduled(cron = "${ai-advice.cleanup-cron:0 25 3 * * *}")
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(Instant.now());
        quotaService.releaseStaleReservations(Instant.now().minusSeconds(600));
    }

    private AiAdviceSession requireSession(String sessionId, String guestId) {
        Optional<String> userId = currentUserService.findCurrentUserId();
        return userId.isPresent()
                ? sessionRepository.findByIdAndUserId(sessionId, userId.get())
                        .orElseThrow(() -> new AppException(AiAdviceErrorCode.SESSION_NOT_FOUND))
                : sessionRepository.findByIdAndGuestKey(sessionId, requireGuestKey(guestId))
                        .orElseThrow(() -> new AppException(AiAdviceErrorCode.SESSION_NOT_FOUND));
    }

    private String requireGuestKey(String guestId) {
        try {
            return UUID.fromString(guestId).toString();
        } catch (RuntimeException ex) {
            throw new AppException(AiAdviceErrorCode.SESSION_NOT_FOUND);
        }
    }

    private AiAdviceMessage saveAssistant(AiAdviceSession session, String content, AiAdviceResponseStatus status,
            AiModelResult<?> result, List<String> suggestedPrompts) {
        return messageRepository.save(AiAdviceMessage.builder().session(session).role(AiAdviceRole.ASSISTANT)
                .content(content).responseStatus(status).provider(result.provider()).model(result.model())
                .inputTokens(result.inputTokens()).outputTokens(result.outputTokens()).latencyMs(result.latencyMs())
                .errorCode(status == AiAdviceResponseStatus.DEGRADED_RECOMMENDATION
                        ? AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE.getCode() : null)
                .suggestedPrompts(writeJson(suggestedPrompts == null ? List.of() : suggestedPrompts)).build());
    }

    private void persistSnapshot(AiAdviceMessage assistant, AiSearchProfile profile, List<Product> products,
            AiAdviceDraft draft) {
        AiAdviceResultSnapshot snapshot = snapshotRepository.save(AiAdviceResultSnapshot.builder().message(assistant)
                .searchProfile(writeJson(profile)).totalResults(products.size()).build());
        assistant.setSnapshot(snapshot);

        Map<Long, AiAdviceDraft.BookReason> reasonByProduct = draft.recommendations().stream()
                .filter(reason -> reason.productId() != null).collect(Collectors
                        .toMap(AiAdviceDraft.BookReason::productId, Function.identity(), (left, right) -> left));
        List<AiAdviceRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            AiAdviceDraft.BookReason reason = reasonByProduct.get(product.getId());
            recommendations.add(AiAdviceRecommendation.builder().snapshot(snapshot).product(product).rankPosition(i + 1)
                    .reason(reason == null ? null : reason.reason())
                    .matchedCriteria(reason == null ? null : writeJson(reason.matchedCriteria())).build());
        }
        recommendationRepository.saveAll(recommendations);
        snapshot.setRecommendations(recommendations);
    }

    private String personalizationContext(String userId) {
        List<Order> orders = orderRepository.findTop10ByUserIdAndOrderStatusOrderByCreatedAtDesc(userId,
                OrderStatus.COMPLETED);
        List<Long> purchased = orders.stream().flatMap(order -> order.getItems().stream()).map(OrderItem::getProductId)
                .distinct().limit(30).toList();
        List<Review> reviews = reviewRepository.findTop20ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        String reviewed = reviews.stream()
                .map(review -> review.getProduct().getProductName() + "=" + review.getRating() + "/5")
                .collect(Collectors.joining(", "));
        List<String> purchasedNames = orders.stream().flatMap(order -> order.getItems().stream())
                .map(OrderItem::getProductName).filter(StringUtils::hasText).distinct().limit(20).toList();
        return "Previously enjoyed book titles: " + purchasedNames + "; recent ratings: " + reviewed;
    }

    private Set<Long> purchasedIds(String userId) {
        return orderRepository.findTop10ByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, OrderStatus.COMPLETED)
                .stream().flatMap(order -> order.getItems().stream()).map(OrderItem::getProductId)
                .collect(Collectors.toSet());
    }

    private List<AiBookCandidate> toCandidates(List<Product> products) {
        return products.stream()
                .map(product -> new AiBookCandidate(product.getId(), product.getProductName(), product.getBookAuthor(),
                        product.getCategory() == null ? null : product.getCategory().getCategoryName(),
                        truncate(product.getDescription(), 600), product.getBookLanguage(), product.getPrice(),
                        product.getAverageRating(), product.getSoldCount()))
                .toList();
    }

    private void recordImpressions(AiAdviceSession session, AiAdviceMessage message,
            List<AiAdviceRecommendation> rows) {
        for (AiAdviceRecommendation row : rows) {
            if (!eventRepository.existsBySessionIdAndRecommendationIdAndEventType(session.getId(), row.getId(),
                    AiAdviceEventType.IMPRESSION)) {
                eventRepository.save(AiAdviceEvent.builder().session(session).message(message).recommendation(row)
                        .eventType(AiAdviceEventType.IMPRESSION).build());
            }
        }
    }

    private AiAdviceSessionResponse toSessionResponse(AiAdviceSession session, List<AiAdviceMessage> messages) {
        List<AiAdviceMessageResponse> responses = messages.stream().map(message -> toMessageResponse(message, false))
                .toList();
        return new AiAdviceSessionResponse(session.getId(), session.getLocale(), session.isPersonalizationEnabled(),
                session.getExpiresAt(), responses, quotaFor(session));
    }

    private AiAdviceMessageResponse toMessageResponse(AiAdviceMessage message, boolean includeQuota) {
        AiAdviceRecommendationPageResponse page = null;
        if (message.getSnapshot() != null) {
            List<AiAdviceRecommendation> rows = message.getSnapshot().getRecommendations().stream()
                    .sorted(Comparator.comparingInt(AiAdviceRecommendation::getRankPosition))
                    .limit(properties.pageSize()).toList();
            page = toPage(message.getSnapshot(), rows, 1);
            if (includeQuota) {
                recordImpressions(message.getSession(), message, rows);
            }
        }
        return new AiAdviceMessageResponse(message.getId(), message.getRole(), message.getContent(),
                message.getResponseStatus(), message.getCreatedAt(), readStringList(message.getSuggestedPrompts()),
                page, includeQuota ? quotaFor(message.getSession()) : null);
    }

    private AiAdviceQuotaResponse quotaFor(AiAdviceSession session) {
        return session.getUser() == null
                ? new AiAdviceQuotaResponse(properties.monthlyLimit(), 0, properties.monthlyLimit(), null)
                : quotaService.getQuota(session.getUser().getId());
    }

    private AiAdviceRecommendationPageResponse toPage(AiAdviceResultSnapshot snapshot,
            List<AiAdviceRecommendation> rows, int page) {
        List<AiAdviceRecommendationResponse> items = rows.stream()
                .map(row -> new AiAdviceRecommendationResponse(row.getId(), row.getRankPosition(),
                        productMapper.toResponse(row.getProduct()), row.getReason(),
                        readStringList(row.getMatchedCriteria())))
                .toList();
        int pages = (int) Math.ceil(snapshot.getTotalResults() / (double) properties.pageSize());
        return new AiAdviceRecommendationPageResponse(items, page, properties.pageSize(), snapshot.getTotalResults(),
                pages, page < pages);
    }

    private AiModelResult<Object> combine(AiModelResult<?> first, AiModelResult<?> second) {
        return new AiModelResult<>(null, second.provider(), second.model(), first.inputTokens() + second.inputTokens(),
                first.outputTokens() + second.outputTokens(), first.latencyMs() + second.latencyMs());
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
        if (!StringUtils.hasText(value))
            return List.of();
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JacksonException ex) {
            return List.of();
        }
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max)
            return value;
        return value.substring(0, max);
    }

}
