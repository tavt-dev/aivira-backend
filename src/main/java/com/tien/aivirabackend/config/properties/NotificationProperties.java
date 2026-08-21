package com.tien.aivirabackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {
    private boolean enabled = true;
    private int pageSizeMax = 100;
    private final Outbox outbox = new Outbox();
    private final Sse sse = new Sse();
    private final Retention retention = new Retention();

    @Data
    public static class Outbox {
        private int batchSize = 100;
        private int maxAttempts = 5;
        private int staleLockMinutes = 5;
    }

    @Data
    public static class Sse {
        private long timeoutMs = 1_800_000;
        private long heartbeatMs = 25_000;
        private int maxConnectionsPerUser = 5;
    }

    @Data
    public static class Retention {
        private int readDays = 90;
        private int unreadDays = 180;
        private int completedOutboxDays = 14;
    }
}
