package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

import com.tien.aivirabackend.domain.dto.request.CouponCreateRequest;
import com.tien.aivirabackend.domain.dto.request.CouponUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.PromotionCreateRequest;
import com.tien.aivirabackend.domain.dto.request.PromotionUpdateRequest;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.discount.CouponService;
import com.tien.aivirabackend.service.discount.PromotionService;

@ExtendWith(MockitoExtension.class)
class CouponPromotionControllerContractTest {
    @Mock
    CouponService couponService;

    @Mock
    PromotionService promotionService;

    MockMvc couponMvc;
    MockMvc promotionMvc;

    @BeforeEach
    void setUp() {
        couponMvc = MockMvcBuilders.standaloneSetup(new AdminCouponController(couponService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        promotionMvc = MockMvcBuilders.standaloneSetup(new AdminPromotionController(promotionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void couponEndpoints_shouldDeclareExpectedPermissions() throws Exception {
        assertPreAuthorize(
                AdminCouponController.class.getMethod("getCoupons", int.class, int.class), "COUPON_MANAGE_ALL");
        assertPreAuthorize(AdminCouponController.class.getMethod("getCoupon", Long.class), "COUPON_MANAGE_ALL");
        assertPreAuthorize(
                AdminCouponController.class.getMethod("createCoupon", CouponCreateRequest.class),
                "COUPON_MANAGE_ALL",
                "COUPON_CREATE_ALL");
        assertPreAuthorize(
                AdminCouponController.class.getMethod("updateCoupon", Long.class, CouponUpdateRequest.class),
                "COUPON_MANAGE_ALL",
                "COUPON_UPDATE_ALL");
        assertPreAuthorize(
                AdminCouponController.class.getMethod("deleteCoupon", Long.class),
                "COUPON_MANAGE_ALL",
                "COUPON_DELETE_ALL");
    }

    @Test
    void promotionEndpoints_shouldDeclareExpectedPermissions() throws Exception {
        assertPreAuthorize(
                AdminPromotionController.class.getMethod("getPromotions", int.class, int.class),
                "PROMOTION_MANAGE_ALL",
                "PROMOTION_READ");
        assertPreAuthorize(
                AdminPromotionController.class.getMethod("getPromotion", Long.class),
                "PROMOTION_MANAGE_ALL",
                "PROMOTION_READ");
        assertPreAuthorize(
                AdminPromotionController.class.getMethod("createPromotion", PromotionCreateRequest.class),
                "PROMOTION_MANAGE_ALL",
                "PROMOTION_CREATE_ALL");
        assertPreAuthorize(
                AdminPromotionController.class.getMethod("updatePromotion", Long.class, PromotionUpdateRequest.class),
                "PROMOTION_MANAGE_ALL",
                "PROMOTION_UPDATE_ALL");
        assertPreAuthorize(
                AdminPromotionController.class.getMethod("deletePromotion", Long.class),
                "PROMOTION_MANAGE_ALL",
                "PROMOTION_DELETE_ALL");
    }

    @Test
    void couponEndpoints_shouldDelegateToService() throws Exception {
        couponMvc
                .perform(get("/admin/coupons").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());
        couponMvc.perform(get("/admin/coupons/7")).andExpect(status().isOk());
        couponMvc
                .perform(
                        post("/admin/coupons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"code":"SAVE10",
								"type":"PERCENT",
								"value":10,
								"startAt":"2026-01-01T00:00:00",
								"endAt":"2026-12-31T23:59:59"
								}
								"""))
                .andExpect(status().isOk());
        couponMvc
                .perform(put("/admin/coupons/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
        couponMvc.perform(delete("/admin/coupons/7")).andExpect(status().isOk());

        verify(couponService).getCoupons(2, 10);
        verify(couponService).getCoupon(7L);
        verify(couponService).createCoupon(any(CouponCreateRequest.class));
        verify(couponService).updateCoupon(eq(7L), any(CouponUpdateRequest.class));
        verify(couponService).deleteCoupon(7L);
    }

    @Test
    void promotionEndpoints_shouldDelegateToService() throws Exception {
        promotionMvc
                .perform(get("/admin/promotions").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());
        promotionMvc.perform(get("/admin/promotions/9")).andExpect(status().isOk());
        promotionMvc
                .perform(
                        post("/admin/promotions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"promotionName":"Book Deal",
								"description":"Selected books",
								"promotionType":"PERCENT",
								"value":10,
								"promotionScope":"PRODUCT",
								"targetId":1,
								"startAt":"2026-01-01T00:00:00",
								"endAt":"2026-12-31T23:59:59"
								}
								"""))
                .andExpect(status().isOk());
        promotionMvc
                .perform(put("/admin/promotions/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
        promotionMvc.perform(delete("/admin/promotions/9")).andExpect(status().isOk());

        verify(promotionService).getPromotions(2, 10);
        verify(promotionService).getPromotion(9L);
        verify(promotionService).createPromotion(any(PromotionCreateRequest.class));
        verify(promotionService).updatePromotion(eq(9L), any(PromotionUpdateRequest.class));
        verify(promotionService).deletePromotion(9L);
    }

    private void assertPreAuthorize(Method method, String... permissions) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        for (String permission : permissions) {
            assertThat(preAuthorize.value()).contains(permission);
        }
    }
}
