package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.VnpayIpnResponse;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;
import com.tien.aivirabackend.service.auth.RequestMetadataService;
import com.tien.aivirabackend.service.payment.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerContractTest {
    @Mock
    PaymentService paymentService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.setFrontendResultUrl("http://localhost:5173/payment-result");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService, new RequestMetadataService(), paymentProperties))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void vnpayReturn_whenProcessed_shouldRedirectToFrontendResultPage() throws Exception {
        when(paymentService.handleVnpayCallback(anyMap(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS));

        mockMvc.perform(get("/payments/vnpay/return").param("vnp_TxnRef", "PAY123-A1").param("vnp_Amount", "123400"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://localhost:5173/payment-result")))
                .andExpect(header().string("Location", containsString("paymentGroupCode=PAY123")))
                .andExpect(header().string("Location", containsString("method=VNPAY")))
                .andExpect(header().string("Location", containsString("status=SUCCESS")));
    }

    @Test
    void vnpayReturn_whenInvalidSignature_shouldRedirectToFrontendFailure() throws Exception {
        doThrow(new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE)).when(paymentService)
                .handleVnpayCallback(anyMap(), org.mockito.ArgumentMatchers.eq(true));

        mockMvc.perform(get("/payments/vnpay/return").param("vnp_TxnRef", "PAY123-A1").param("vnp_Amount", "123400"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://localhost:5173/payment-result")))
                .andExpect(header().string("Location", containsString("paymentGroupCode=PAY123")))
                .andExpect(header().string("Location", containsString("method=VNPAY")))
                .andExpect(header().string("Location", containsString("status=FAILED")))
                .andExpect(header().string("Location", containsString("errorCode=PAYMENT-003")));
    }

    @Test
    void momoReturn_whenProcessed_shouldRedirectToFrontendResultPage() throws Exception {
        when(paymentService.handleMomoReturn(anyMap()))
                .thenReturn(paymentGroup(PaymentMethod.MOMO, PaymentStatus.SUCCESS));

        mockMvc.perform(get("/payments/momo/return").param("orderId", "PAY123-A1").param("amount", "1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://localhost:5173/payment-result")))
                .andExpect(header().string("Location", containsString("paymentGroupCode=PAY123")))
                .andExpect(header().string("Location", containsString("method=MOMO")))
                .andExpect(header().string("Location", containsString("status=SUCCESS")));
    }

    @Test
    void momoReturn_whenInvalidSignature_shouldRedirectToFrontendFailure() throws Exception {
        doThrow(new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE)).when(paymentService)
                .handleMomoReturn(anyMap());

        mockMvc.perform(get("/payments/momo/return").param("orderId", "PAY123-A1").param("amount", "1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://localhost:5173/payment-result")))
                .andExpect(header().string("Location", containsString("paymentGroupCode=PAY123")))
                .andExpect(header().string("Location", containsString("method=MOMO")))
                .andExpect(header().string("Location", containsString("status=FAILED")))
                .andExpect(header().string("Location", containsString("errorCode=PAYMENT-003")));
    }

    @Test
    void vnpayIpn_shouldReturnProviderContractWithoutApiResponseWrapper() throws Exception {
        when(paymentService.handleVnpayIpn(anyMap())).thenReturn(new VnpayIpnResponse("00", "Confirm Success"));

        mockMvc.perform(get("/payments/vnpay/ipn").param("vnp_TxnRef", "PAY123-A1").param("vnp_Amount", "123400"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void momoIpn_whenAccepted_shouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/payments/momo/ipn").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"PAY123-A1\"}")).andExpect(status().isNoContent());

        verify(paymentService).handleMomoIpn(anyMap());
    }

    @Test
    void momoIpn_whenInvalidSignature_shouldReturnBadRequest() throws Exception {
        doThrow(new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE)).when(paymentService)
                .handleMomoIpn(anyMap());

        mockMvc.perform(post("/payments/momo/ipn").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"PAY123-A1\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.errorCode").value("PAYMENT-003"));
    }

    @Test
    void reconcile_shouldRequirePaymentReconcilePermission() throws Exception {
        Method method = PaymentController.class.getMethod("reconcile", String.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("PAYMENT_RECONCILE");
    }

    private PaymentGroupResponse paymentGroup(PaymentMethod method, PaymentStatus status) {
        return PaymentGroupResponse.builder().paymentCode("PAY123").method(method).status(status).build();
    }
}
