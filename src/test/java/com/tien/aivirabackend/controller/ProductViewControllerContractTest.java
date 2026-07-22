package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.tien.aivirabackend.domain.dto.request.ProductViewRequest;

class ProductViewControllerContractTest {
    @Test
    void trackingEndpoint_shouldRemainPublicAtMethodLevel() throws Exception {
        var method = ProductController.class.getMethod("recordProductView", String.class, ProductViewRequest.class);
        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void currentUserHistoryEndpoints_shouldExist() throws Exception {
        assertThat(UserController.class.getMethod("getRecentlyViewed", int.class, int.class))
                .isNotNull();
        assertThat(UserController.class.getMethod("removeRecentlyViewed", Long.class))
                .isNotNull();
        assertThat(UserController.class.getMethod("clearRecentlyViewed")).isNotNull();
    }
}
