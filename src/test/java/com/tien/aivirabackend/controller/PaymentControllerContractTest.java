package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.tien.aivirabackend.domain.dto.response.VnpayIpnResponse;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;
import com.tien.aivirabackend.service.PaymentService;
import com.tien.aivirabackend.service.RequestMetadataService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerContractTest {
    @Mock
    PaymentService paymentService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService, new RequestMetadataService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void vnpayIpn_shouldReturnProviderContractWithoutApiResponseWrapper() throws Exception {
        when(paymentService.handleVnpayIpn(anyMap())).thenReturn(new VnpayIpnResponse("00", "Confirm Success"));

        mockMvc.perform(get("/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "PAY123-A1")
                        .param("vnp_Amount", "123400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void momoIpn_whenAccepted_shouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"PAY123-A1\"}"))
                .andExpect(status().isNoContent());

        verify(paymentService).handleMomoIpn(anyMap());
    }

    @Test
    void momoIpn_whenInvalidSignature_shouldReturnBadRequest() throws Exception {
        doThrow(new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE))
                .when(paymentService)
                .handleMomoIpn(anyMap());

        mockMvc.perform(post("/payments/momo/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"PAY123-A1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PAYMENT-003"));
    }

    @Test
    void reconcile_shouldRequirePaymentReconcilePermission() throws Exception {
        Method method = PaymentController.class.getMethod("reconcile", String.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("PAYMENT_RECONCILE");
    }
}
