package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;

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
class OrderControllerContractTest {
    @Mock
    OrderService orderService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getMyOrders_shouldRequireOrderReadSelfPermission() throws Exception {
        Method method = OrderController.class.getMethod("getMyOrders", OrderStatus.class, int.class, int.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ORDER_READ_SELF");
    }

    @Test
    void getMyOrder_shouldRequireOrderReadSelfPermission() throws Exception {
        Method method = OrderController.class.getMethod("getMyOrder", Long.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ORDER_READ_SELF");
    }

    @Test
    void cancelMyOrder_shouldRequireOrderCancelSelfPermission() throws Exception {
        Method method = OrderController.class.getMethod("cancelMyOrder", Long.class, OrderCancelRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ORDER_CANCEL_SELF");
    }

    @Test
    void endpoints_shouldDelegateToOrderService() throws Exception {
        mockMvc.perform(get("/orders").param("page", "1").param("size", "20")).andExpect(status().isOk());
        mockMvc.perform(get("/orders/21")).andExpect(status().isOk());
        mockMvc.perform(post("/orders/21/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"wrong address\"}")).andExpect(status().isOk());

        verify(orderService).getMyOrders(null, 1, 20);
        verify(orderService).getMyOrder(21L);
        verify(orderService).cancelMyOrder(eq(21L), any(OrderCancelRequest.class));
    }

    @Test
    void cancelMyOrder_whenReasonTooLong_shouldReturnValidationError() throws Exception {
        String reason = "a".repeat(501);

        mockMvc.perform(post("/orders/21/cancel").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + reason + "\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.errorCode").value("E1100"))
                .andExpect(jsonPath("$.data.reason").exists());
    }
}
