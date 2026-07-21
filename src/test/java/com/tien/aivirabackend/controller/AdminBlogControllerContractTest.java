package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.tien.aivirabackend.domain.dto.request.BlogCategoryRequest;
import com.tien.aivirabackend.domain.dto.request.BlogPostCreateRequest;

class AdminBlogControllerContractTest {
    @Test
    void mutatingEndpoints_shouldDeclareCmsPermissions() throws Exception {
        assertPermission(
                AdminBlogController.class.getMethod("createCategory", BlogCategoryRequest.class), "CMS_CREATE");
        assertPermission(AdminBlogController.class.getMethod("createPost", BlogPostCreateRequest.class), "CMS_CREATE");
        assertPermission(AdminBlogController.class.getMethod("publishPost", Long.class), "CMS_UPDATE");
        assertPermission(AdminBlogController.class.getMethod("deletePost", Long.class), "CMS_DELETE");
    }

    private void assertPermission(Method method, String permission) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(permission, "CMS_MANAGE_ALL");
    }
}
