package com.tien.aivirabackend.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.repository.projection.DailyOrderCountProjection;
import com.tien.aivirabackend.repository.projection.OrderStatusCountProjection;
import com.tien.aivirabackend.repository.projection.SalesPointProjection;
import com.tien.aivirabackend.repository.projection.TopBookProjection;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock
    ProductRepository productRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    UserRepository userRepository;

    DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService =
                new DashboardServiceImpl(productRepository, orderRepository, paymentRepository, userRepository);
    }

    @Test
    void getSummary_shouldCalculateDashboardMetrics() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        when(paymentRepository.sumSuccessfulRevenueBetween(from, to)).thenReturn(new BigDecimal("1200.00"));
        when(orderRepository.countByCreatedAtBetween(from, to)).thenReturn(9L);
        when(paymentRepository.countByStatusInAndCreatedAtBetween(anyCollection(), eq(from), eq(to)))
                .thenAnswer(invocation -> {
                    java.util.Collection<PaymentStatus> statuses = invocation.getArgument(0);
                    return statuses.contains(PaymentStatus.SUCCESS) ? 6L : 3L;
                });
        when(userRepository.countByIsDeletedFalseAndCreatedAtBetween(from, to)).thenReturn(4L);
        when(orderRepository.countByOrderStatusIn(anyCollection())).thenReturn(5L);
        when(paymentRepository.countByStatus(PaymentStatus.PENDING)).thenReturn(2L);
        when(productRepository.countByActiveTrueAndStatusAndStockQuantityLessThanEqual(ProductStatus.ACTIVE, 5))
                .thenReturn(7L);

        var response = dashboardService.getSummary(from, to);

        assertThat(response.getRevenue()).isEqualByComparingTo("1200.00");
        assertThat(response.getOrderCount()).isEqualTo(9L);
        assertThat(response.getSuccessfulPaymentCount()).isEqualTo(6L);
        assertThat(response.getFailedPaymentCount()).isEqualTo(3L);
        assertThat(response.getNewUserCount()).isEqualTo(4L);
        assertThat(response.getPendingOrderCount()).isEqualTo(5L);
        assertThat(response.getPendingPaymentCount()).isEqualTo(2L);
        assertThat(response.getLowStockCount()).isEqualTo(7L);
    }

    @Test
    void getSummary_whenRangeMissing_shouldUseDefaultLastThirtyDays() {
        when(paymentRepository.sumSuccessfulRevenueBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);

        dashboardService.getSummary(null, null);

        verify(orderRepository).countByCreatedAtBetween(any(Instant.class), any(Instant.class));
        verify(userRepository).countByIsDeletedFalseAndCreatedAtBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    void getSummary_whenInvalidRange_shouldReject() {
        assertThatThrownBy(() -> dashboardService.getSummary(
                        Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(CommonErrorCode.INVALID_INPUT);
    }

    @Test
    void getSales_shouldMergeRevenueAndOrderCountsByDate() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T23:59:59Z");
        when(paymentRepository.aggregateDailySuccessfulRevenue(from, to))
                .thenReturn(List.of(sales(LocalDate.parse("2026-01-01"), new BigDecimal("100.00"))));
        when(orderRepository.countDailyOrdersBetween(from, to))
                .thenReturn(List.of(orders(LocalDate.parse("2026-01-02"), 3L)));

        var response = dashboardService.getSales(from, to);

        assertThat(response.getPoints()).hasSize(2);
        assertThat(response.getPoints().get(0).getRevenue()).isEqualByComparingTo("100.00");
        assertThat(response.getPoints().get(0).getOrderCount()).isZero();
        assertThat(response.getPoints().get(1).getRevenue()).isEqualByComparingTo("0");
        assertThat(response.getPoints().get(1).getOrderCount()).isEqualTo(3L);
    }

    @Test
    void getOrders_shouldReturnAllStatusesWithZeroFallback() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        when(orderRepository.countOrdersByStatusBetween(from, to))
                .thenReturn(List.of(statusCount(OrderStatus.COMPLETED, 8L)));

        var response = dashboardService.getOrders(from, to);

        assertThat(response.getStatusCounts()).hasSize(OrderStatus.values().length);
        assertThat(response.getStatusCounts()).anySatisfy(item -> {
            assertThat(item.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(item.getCount()).isEqualTo(8L);
        });
    }

    @Test
    void getTopBooks_shouldUseOrderItemAggregates() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        when(orderRepository.findTopBooksBetween(anyCollection(), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(List.of(topBook(10L, "Book", 4L, new BigDecimal("400.00"))));

        var response = dashboardService.getTopBooks(from, to, 100);

        assertThat(response.getBooks()).hasSize(1);
        assertThat(response.getBooks().getFirst().getQuantitySold()).isEqualTo(4L);
        verify(orderRepository)
                .findTopBooksBetween(
                        anyCollection(), eq(from), eq(to), argThat(pageable -> pageable.getPageSize() == 50));
        verify(productRepository, never()).findByActiveTrueAndStatusOrderBySoldCountDescCreatedAtDesc(any(), any());
    }

    @Test
    void getLowStock_shouldRespectThresholdAndLimit() {
        when(productRepository.findByActiveTrueAndStatusAndStockQuantityLessThanEqual(
                        eq(ProductStatus.ACTIVE), eq(3), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(Product.builder()
                        .id(1L)
                        .productName("Low")
                        .slug("low")
                        .sku("LOW")
                        .stockQuantity(2)
                        .build())));

        var response = dashboardService.getLowStock(3, 4);

        assertThat(response.getBooks()).hasSize(1);
        assertThat(response.getBooks().getFirst().getStockQuantity()).isEqualTo(2);
        verify(productRepository)
                .findByActiveTrueAndStatusAndStockQuantityLessThanEqual(
                        eq(ProductStatus.ACTIVE), eq(3), argThat(pageable -> pageable.getPageSize() == 4));
    }

    private SalesPointProjection sales(LocalDate date, BigDecimal revenue) {
        return new SalesPointProjection() {
            @Override
            public Object getSalesDate() {
                return date;
            }

            @Override
            public BigDecimal getRevenue() {
                return revenue;
            }
        };
    }

    private DailyOrderCountProjection orders(LocalDate date, Long count) {
        return new DailyOrderCountProjection() {
            @Override
            public Object getOrderDate() {
                return date;
            }

            @Override
            public Long getOrderCount() {
                return count;
            }
        };
    }

    private OrderStatusCountProjection statusCount(OrderStatus status, Long count) {
        return new OrderStatusCountProjection() {
            @Override
            public OrderStatus getStatus() {
                return status;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private TopBookProjection topBook(Long id, String name, Long quantity, BigDecimal revenue) {
        return new TopBookProjection() {
            @Override
            public Long getProductId() {
                return id;
            }

            @Override
            public String getProductName() {
                return name;
            }

            @Override
            public String getSku() {
                return "SKU";
            }

            @Override
            public String getThumbnailUrl() {
                return null;
            }

            @Override
            public Long getQuantitySold() {
                return quantity;
            }

            @Override
            public BigDecimal getRevenue() {
                return revenue;
            }
        };
    }
}
