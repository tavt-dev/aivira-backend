package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.commerce.OrderService;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerContractTest {
    @Mock
    OrderService orderService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listAndDetail_shouldRequireManageOrReadAllPermission() throws Exception {
        assertPreAuthorize(
                AdminOrderController.class.getMethod(
                        "getAdminOrders",
                        OrderStatus.class,
                        String.class,
                        Instant.class,
                        Instant.class,
                        int.class,
                        int.class),
                "ORDER_MANAGE_ALL",
                "ORDER_READ_ALL");
        assertPreAuthorize(
                AdminOrderController.class.getMethod("getAdminOrder", Long.class),
                "ORDER_MANAGE_ALL",
                "ORDER_READ_ALL");
    }

    @Test
    void statusTransitions_shouldRequireManageOrUpdateStatusPermission() throws Exception {
        for (String methodName : new String[] {"confirmOrder", "markPacking", "markShipping", "markCompleted"}) {
            assertPreAuthorize(
                    AdminOrderController.class.getMethod(methodName, Long.class),
                    "ORDER_MANAGE_ALL",
                    "ORDER_UPDATE_STATUS_ALL");
        }
    }

    @Test
    void cancel_shouldRequireManageOrCancelAllPermission() throws Exception {
        assertPreAuthorize(
                AdminOrderController.class.getMethod("cancelAdminOrder", Long.class, OrderCancelRequest.class),
                "ORDER_MANAGE_ALL",
                "ORDER_CANCEL_ALL");
    }

    @Test
    void endpoints_shouldDelegateToOrderService() throws Exception {
        mockMvc.perform(get("/admin/orders")
                        .param("status", "CONFIRMED")
                        .param("keyword", "ORD")
                        .param("fromDate", "2026-01-01T00:00:00Z")
                        .param("toDate", "2026-12-31T23:59:59Z")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/orders/21")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/orders/21/confirm")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/orders/21/packing")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/orders/21/shipping")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/orders/21/completed")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/orders/21/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"customer requested\"}"))
                .andExpect(status().isOk());

        verify(orderService)
                .getAdminOrders(
                        eq(OrderStatus.CONFIRMED),
                        eq("ORD"),
                        eq(Instant.parse("2026-01-01T00:00:00Z")),
                        eq(Instant.parse("2026-12-31T23:59:59Z")),
                        eq(2),
                        eq(10));
        verify(orderService).getAdminOrder(21L);
        verify(orderService).confirmOrder(21L);
        verify(orderService).markPacking(21L);
        verify(orderService).markShipping(21L);
        verify(orderService).markCompleted(21L);
        verify(orderService).cancelAdminOrder(eq(21L), any(OrderCancelRequest.class));
    }

    private void assertPreAuthorize(Method method, String... permissions) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        for (String permission : permissions) {
            assertThat(preAuthorize.value()).contains(permission);
        }
    }
}
