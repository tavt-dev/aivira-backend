package com.tien.aivirabackend.config.properties;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Data
@Validated
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    @Min(1)
    private long pendingTtlMinutes = 15;

    private String frontendResultUrl = "http://localhost:5173/payment-result";

    @Min(100)
    private int providerConnectTimeoutMs = 5000;

    @Min(100)
    private int providerReadTimeoutMs = 30000;
}
