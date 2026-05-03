package com.tien.aivirabackend.domain.dto;

public record RequestMetadata(String userAgent, String remoteAddress) {
    public String clientIp() {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "127.0.0.1";
        }
        String firstForwardedIp = remoteAddress.split(",")[0].trim();
        return firstForwardedIp.isBlank() ? "127.0.0.1" : firstForwardedIp;
    }
}
