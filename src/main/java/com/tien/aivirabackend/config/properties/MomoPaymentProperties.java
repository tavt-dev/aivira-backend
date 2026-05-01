package com.tien.aivirabackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "payment.momo")
public class MomoPaymentProperties {
    private boolean enabled = false;
    private String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String partnerCode = "";
    private String accessKey = "";
    private String secretKey = "";
    private String redirectUrl = "";
    private String ipnUrl = "";
    private String requestType = "payWithMethod";
}
