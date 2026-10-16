package com.smsmurilo.notify.api;

import com.smsmurilo.notify.channel.NotificationDispatcher;
import com.smsmurilo.notify.domain.DeliveryStatus;
import com.smsmurilo.notify.domain.Notification;
import com.smsmurilo.notify.messaging.NotificationPublisher;
import com.smsmurilo.notify.ratelimit.TenantRateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private final TenantRateLimiter rateLimiter;
    private final NotificationPublisher publisher;
    private final NotificationDispatcher dispatcher;

    NotificationController(TenantRateLimiter rateLimiter, NotificationPublisher publisher, NotificationDispatcher dispatcher) {
        this.rateLimiter = rateLimiter;
        this.publisher = publisher;
        this.dispatcher = dispatcher;
    }

    record SendRequest(@NotNull Notification.Channel channel, @NotBlank String recipient, @NotBlank String template,
                       Map<String, String> variables, Notification.Priority priority) {}

    record Accepted(UUID id, String status) {}

    @PostMapping
    Mono<ResponseEntity<Accepted>> send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SendRequest req) {
        // Tenant comes from the token, never from the payload: a client can't spend another tenant's quota.
        var tenant = jwt.getClaimAsString("tenant");
        return rateLimiter.tryAcquire(tenant).flatMap(allowed -> {
            if (!allowed) {
                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).<Accepted>build());
            }
            var n = new Notification(UUID.randomUUID(), tenant, req.channel(), req.recipient(), req.template(),
                    req.variables() == null ? Map.of() : req.variables(),
                    req.priority() == null ? Notification.Priority.NORMAL : req.priority(), Instant.now());
            return publisher.publish(n)
                    .doOnNext(dispatcher::queued)
                    .map(sent -> ResponseEntity.accepted().body(new Accepted(sent.id(), "QUEUED")));
        });
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<DeliveryStatus> stream(@AuthenticationPrincipal Jwt jwt) {
        return dispatcher.statusStream(jwt.getClaimAsString("tenant"));
    }
}
