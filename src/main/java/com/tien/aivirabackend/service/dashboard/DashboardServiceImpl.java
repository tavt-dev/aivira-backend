package com.tien.aivirabackend.service.dashboard;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.response.DashboardLowStockResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardOrdersResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardSalesResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardSummaryResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardTopBooksResponse;
import com.tien.aivirabackend.domain.dto.response.LowStockBookResponse;
import com.tien.aivirabackend.domain.dto.response.OrderStatusCountResponse;
import com.tien.aivirabackend.domain.dto.response.SalesPointResponse;
import com.tien.aivirabackend.domain.dto.response.TopBookResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.repository.projection.DailyOrderCountProjection;
import com.tien.aivirabackend.repository.projection.TopBookProjection;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardServiceImpl implements DashboardService {
    static final int DEFAULT_RANGE_DAYS = 30;
    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 50;
    static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    static final EnumSet<OrderStatus> PENDING_ORDER_STATUSES = EnumSet.of(OrderStatus.PENDING_CONFIRMATION,
            OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.PACKING);
    static final EnumSet<OrderStatus> TOP_BOOK_EXCLUDED_STATUSES = EnumSet.of(OrderStatus.CANCELLED,
            OrderStatus.PAYMENT_FAILED, OrderStatus.EXPIRED, OrderStatus.REFUNDED);
    static final EnumSet<PaymentStatus> FAILED_PAYMENT_STATUSES = EnumSet.of(PaymentStatus.FAILED,
            PaymentStatus.CANCELLED, PaymentStatus.EXPIRED);

    ProductRepository productRepository;
    OrderRepository orderRepository;
    PaymentRepository paymentRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Instant fromDate, Instant toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        int lowStockThreshold = DEFAULT_LOW_STOCK_THRESHOLD;
        return DashboardSummaryResponse.builder()
                .revenue(defaultMoney(paymentRepository.sumSuccessfulRevenueBetween(range.fromDate(), range.toDate())))
                .orderCount(orderRepository.countByCreatedAtBetween(range.fromDate(), range.toDate()))
                .successfulPaymentCount(paymentRepository.countByStatusInAndCreatedAtBetween(
                        java.util.List.of(PaymentStatus.SUCCESS), range.fromDate(), range.toDate()))
                .failedPaymentCount(paymentRepository.countByStatusInAndCreatedAtBetween(FAILED_PAYMENT_STATUSES,
                        range.fromDate(), range.toDate()))
                .newUserCount(userRepository.countByIsDeletedFalseAndCreatedAtBetween(range.fromDate(), range.toDate()))
                .pendingOrderCount(orderRepository.countByOrderStatusIn(PENDING_ORDER_STATUSES))
                .pendingPaymentCount(paymentRepository.countByStatus(PaymentStatus.PENDING))
                .lowStockCount(productRepository.countByActiveTrueAndStatusAndStockQuantityLessThanEqual(
                        ProductStatus.ACTIVE, lowStockThreshold))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSalesResponse getSales(Instant fromDate, Instant toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        Map<LocalDate, BigDecimal> revenueByDate = paymentRepository
                .aggregateDailySuccessfulRevenue(range.fromDate(), range.toDate()).stream()
                .collect(Collectors.toMap(projection -> toLocalDate(projection.getSalesDate()),
                        projection -> defaultMoney(projection.getRevenue())));
        Map<LocalDate, Long> orderCountByDate = orderRepository
                .countDailyOrdersBetween(range.fromDate(), range.toDate()).stream()
                .collect(Collectors.toMap(projection -> toLocalDate(projection.getOrderDate()),
                        DailyOrderCountProjection::getOrderCount));

        LocalDate start = range.fromDate().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = range.toDate().atZone(ZoneOffset.UTC).toLocalDate();
        java.util.List<SalesPointResponse> points = start.datesUntil(end.plusDays(1))
                .map(date -> SalesPointResponse.builder().date(date)
                        .revenue(revenueByDate.getOrDefault(date, BigDecimal.ZERO))
                        .orderCount(orderCountByDate.getOrDefault(date, 0L)).build())
                .toList();

        return DashboardSalesResponse.builder().fromDate(range.fromDate()).toDate(range.toDate()).points(points)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOrdersResponse getOrders(Instant fromDate, Instant toDate) {
        DateRange range = resolveRange(fromDate, toDate);
        Map<OrderStatus, Long> counts = orderRepository.countOrdersByStatusBetween(range.fromDate(), range.toDate())
                .stream()
                .collect(Collectors.toMap(
                        com.tien.aivirabackend.repository.projection.OrderStatusCountProjection::getStatus,
                        com.tien.aivirabackend.repository.projection.OrderStatusCountProjection::getCount));

        return DashboardOrdersResponse.builder()
                .statusCounts(java.util.Arrays.stream(OrderStatus.values()).map(status -> OrderStatusCountResponse
                        .builder().status(status).count(counts.getOrDefault(status, 0L)).build()).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardTopBooksResponse getTopBooks(Instant fromDate, Instant toDate, int limit) {
        DateRange range = resolveRange(fromDate, toDate);
        int normalizedLimit = normalizeLimit(limit);
        java.util.List<TopBookResponse> books = orderRepository.findTopBooksBetween(TOP_BOOK_EXCLUDED_STATUSES,
                range.fromDate(), range.toDate(), PageRequest.of(0, normalizedLimit)).stream().map(this::toTopBook)
                .toList();

        if (books.isEmpty()) {
            books = productRepository
                    .findByActiveTrueAndStatusOrderBySoldCountDescCreatedAtDesc(ProductStatus.ACTIVE,
                            PageRequest.of(0, normalizedLimit))
                    .stream().filter(product -> product.getSoldCount() != null && product.getSoldCount() > 0)
                    .map(this::toTopBookFallback).toList();
        }

        return DashboardTopBooksResponse.builder().books(books).build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardLowStockResponse getLowStock(Integer threshold, int limit) {
        int normalizedThreshold = threshold == null ? DEFAULT_LOW_STOCK_THRESHOLD : Math.max(threshold, 0);
        int normalizedLimit = normalizeLimit(limit);
        return DashboardLowStockResponse.builder().books(productRepository
                .findByActiveTrueAndStatusAndStockQuantityLessThanEqual(ProductStatus.ACTIVE, normalizedThreshold,
                        PageRequest.of(0, normalizedLimit,
                                Sort.by(Sort.Direction.ASC, "stockQuantity")
                                        .and(Sort.by(Sort.Direction.ASC, "productName"))))
                .stream().map(this::toLowStockBook).toList()).build();
    }

    private DateRange resolveRange(Instant fromDate, Instant toDate) {
        Instant resolvedToDate = toDate == null ? Instant.now() : toDate;
        Instant resolvedFromDate = fromDate == null ? resolvedToDate.minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS)
                : fromDate;
        if (resolvedFromDate.isAfter(resolvedToDate)) {
            throw new AppException(CommonErrorCode.INVALID_INPUT);
        }
        return new DateRange(resolvedFromDate, resolvedToDate);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private TopBookResponse toTopBook(TopBookProjection projection) {
        return TopBookResponse.builder().productId(projection.getProductId()).productName(projection.getProductName())
                .sku(projection.getSku()).thumbnailUrl(projection.getThumbnailUrl())
                .quantitySold(projection.getQuantitySold() == null ? 0L : projection.getQuantitySold())
                .revenue(defaultMoney(projection.getRevenue())).build();
    }

    private TopBookResponse toTopBookFallback(Product product) {
        return TopBookResponse.builder().productId(product.getId()).productName(product.getProductName())
                .sku(product.getSku()).thumbnailUrl(product.getThumbnailUrl())
                .quantitySold(product.getSoldCount() == null ? 0L : product.getSoldCount().longValue())
                .revenue(BigDecimal.ZERO).build();
    }

    private LowStockBookResponse toLowStockBook(Product product) {
        return LowStockBookResponse.builder().productId(product.getId()).productName(product.getProductName())
                .slug(product.getSlug()).sku(product.getSku()).thumbnailUrl(product.getThumbnailUrl())
                .stockQuantity(product.getStockQuantity()).build();
    }

    private record DateRange(Instant fromDate, Instant toDate) {
    }
}
