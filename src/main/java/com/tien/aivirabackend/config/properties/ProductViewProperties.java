package com.tien.aivirabackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "analytics.product-view")
public class ProductViewProperties {
    private boolean enabled = true;
    private int deduplicationMinutes = 15;
    private int anonymousRetentionDays = 90;
    private int eventRetentionDays = 365;
    private int recentRetentionDays = 180;
    private int maxRecentProducts = 100;
    private int minuteRateLimit = 60;
    private int hourlyRateLimit = 300;
    private String hashPepper = "local-product-view-pepper";
}
