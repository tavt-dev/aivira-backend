package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlogErrorCode implements ErrorCode {
    BLOG_POST_NOT_FOUND("BLOG-001", "Blog post not found", HttpStatus.NOT_FOUND),
    BLOG_CATEGORY_NOT_FOUND("BLOG-002", "Blog category not found", HttpStatus.NOT_FOUND),
    BLOG_SLUG_ALREADY_EXISTS("BLOG-003", "Blog slug already exists", HttpStatus.CONFLICT),
    BLOG_INVALID_STATUS("BLOG-004", "Blog post status transition is invalid", HttpStatus.BAD_REQUEST),
    BLOG_PUBLISH_VALIDATION_FAILED("BLOG-005", "Blog post is not ready to publish", HttpStatus.BAD_REQUEST),
    BLOG_CATEGORY_IN_USE("BLOG-006", "Blog category is still in use", HttpStatus.CONFLICT),
    BLOG_ASSET_NOT_FOUND("BLOG-007", "Blog asset not found", HttpStatus.NOT_FOUND),
    BLOG_IMAGE_UPLOAD_FAILED("BLOG-008", "Blog image upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    BLOG_RELATED_PRODUCT_NOT_FOUND("BLOG-009", "Related product not found", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
