package com.tien.aivirabackend.config.properties;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Data
@Validated
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {
    @NotBlank
    private String cloudName;

    @NotBlank
    private String apiKey;

    @NotBlank
    private String apiSecret;

    private boolean secure = true;

    private String avatarFolder = "aivira/users";

    private String productMediaFolder = "aivira/products";
}
