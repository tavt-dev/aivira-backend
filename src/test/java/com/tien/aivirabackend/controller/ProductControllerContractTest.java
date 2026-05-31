package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductMediaUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.StockUpdateRequest;

class ProductControllerContractTest {
    @Test
    void adminProductEndpoints_shouldRequireAdminProductPermissions() throws Exception {
        assertPreAuthorizeContains(
                ProductController.class.getMethod("getAdminProducts", ProductStatus.class, Long.class, String.class, int.class, int.class),
                "PRODUCT_MANAGE_ALL");
        assertPreAuthorizeContains(
                ProductController.class.getMethod("createAdminProduct", ProductCreateRequest.class),
                "PRODUCT_MANAGE_ALL");
        assertPreAuthorizeContains(
                ProductController.class.getMethod("updateAdminProduct", Long.class, ProductUpdateRequest.class),
                "PRODUCT_MANAGE_ALL");
        assertPreAuthorizeContains(
                ProductController.class.getMethod("deleteAdminProduct", Long.class),
                "PRODUCT_MANAGE_ALL");
        assertPreAuthorizeContains(
                ProductController.class.getMethod(
                        "updateProductMedia", Long.class, Long.class, ProductMediaUpdateRequest.class),
                "PRODUCT_MEDIA_MANAGE_ALL");
        assertPreAuthorizeContains(
                ProductController.class.getMethod(
                        "updateVariationStock", Long.class, Long.class, StockUpdateRequest.class),
                "INVENTORY_MANAGE_ALL");
    }

    private void assertPreAuthorizeContains(Method method, String permissionCode) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains(permissionCode);
    }
}
