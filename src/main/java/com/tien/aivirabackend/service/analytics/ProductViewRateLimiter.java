package com.tien.aivirabackend.service.analytics;

public interface ProductViewRateLimiter {
    boolean allow(String viewerKey);
}
