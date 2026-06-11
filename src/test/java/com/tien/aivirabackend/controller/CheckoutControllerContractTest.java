package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.dto.request.CheckoutRequest;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.auth.RequestMetadataService;
import com.tien.aivirabackend.service.commerce.CheckoutService;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerContractTest {
    @Mock
    CheckoutService checkoutService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CheckoutController(checkoutService, new RequestMetadataService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void previewAndCheckout_shouldRequireCheckoutCreateSelfPermission() throws Exception {
        assertPreAuthorize(
                CheckoutController.class.getMethod("preview", CheckoutRequest.class), "CHECKOUT_CREATE_SELF");
        assertPreAuthorize(
                CheckoutController.class.getMethod("checkout", CheckoutRequest.class, HttpServletRequest.class),
                "CHECKOUT_CREATE_SELF");
    }

    @Test
    void preview_shouldAcceptCouponCodeAndDelegateToService() throws Exception {
        mockMvc.perform(post("/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isOk());

        verify(checkoutService).preview(argThat(request -> "SAVE10".equals(request.getCouponCode())));
    }

    @Test
    void checkout_shouldAcceptCouponCodeAndDelegateToService() throws Exception {
        mockMvc.perform(post("/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutJson()))
                .andExpect(status().isOk());

        verify(checkoutService)
                .checkout(argThat(request -> "SAVE10".equals(request.getCouponCode())), any(RequestMetadata.class));
    }

    private String validCheckoutJson() {
        return """
				{
				"addressId":1,
				"cartItemIds":[10,11],
				"paymentMethod":"COD",
				"couponCode":"SAVE10",
				"notes":"deliver after 6pm"
				}
				""";
    }

    private void assertPreAuthorize(Method method, String permission) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains(permission);
    }
}
