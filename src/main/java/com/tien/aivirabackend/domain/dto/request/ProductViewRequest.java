package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.ProductViewSource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductViewRequest {
    @Size(max = 36)
    private String anonymousId;

    @Size(max = 36)
    private String sessionId;

    private ProductViewSource source = ProductViewSource.DIRECT;

    @Size(max = 500)
    private String referrerPath;
}
