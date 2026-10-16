package com.smsmurilo.notify.domain;

import java.time.Instant;
import java.util.UUID;

public record DeliveryStatus(UUID notificationId, String tenant, State state, String detail, Instant at) {
    public enum State { QUEUED, SENT, FAILED }
}
