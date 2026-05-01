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
}
