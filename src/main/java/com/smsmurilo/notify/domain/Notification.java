package com.smsmurilo.notify.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Notification(
        UUID id,
        String tenant,
        Channel channel,
        String recipient,
        String template,
        Map<String, String> variables,
        Priority priority,
        Instant requestedAt) {

    public enum Channel { SMS, EMAIL, PUSH }

    public enum Priority { HIGH, NORMAL }
}
