package com.smsmurilo.notify;

import com.smsmurilo.notify.channel.NotificationDispatcher;
import com.smsmurilo.notify.domain.Notification;
import com.smsmurilo.notify.messaging.NotificationPublisher;
import com.smsmurilo.notify.ratelimit.TenantRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest
@Import(com.smsmurilo.notify.config.SecurityConfig.class)
class NotificationControllerTest {

    @Autowired WebTestClient client;
    @MockitoBean TenantRateLimiter rateLimiter;
    @MockitoBean NotificationPublisher publisher;
    @MockitoBean NotificationDispatcher dispatcher;

    private final Map<String, Object> body = Map.of("channel", "SMS", "recipient", "+5582999990000",
            "template", "codigo-acesso", "variables", Map.of("codigo", "482913"), "priority", "HIGH");

    private WebTestClient asTenant(String tenant, String scope) {
        return client.mutateWith(mockJwt().jwt(j -> j.claim("tenant", tenant).claim("scope", scope)));
    }

    @Test
    void acceptsAndQueuesWhenWithinRateLimit() {
        when(rateLimiter.tryAcquire("hospital-a")).thenReturn(Mono.just(true));
        when(publisher.publish(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        asTenant("hospital-a", "notifications:send").post().uri("/api/notifications").bodyValue(body)
                .exchange().expectStatus().isAccepted().expectBody().jsonPath("$.status").isEqualTo("QUEUED");

        verify(publisher).publish(argThat((Notification n) -> n.tenant().equals("hospital-a")
                && n.priority() == Notification.Priority.HIGH));
    }

    @Test
    void returns429WhenTenantExceedsQuota() {
        when(rateLimiter.tryAcquire(eq("hospital-a"))).thenReturn(Mono.just(false));

        asTenant("hospital-a", "notifications:send").post().uri("/api/notifications").bodyValue(body)
                .exchange().expectStatus().isEqualTo(429);
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsTokenWithoutScope() {
        asTenant("hospital-a", "profile").post().uri("/api/notifications").bodyValue(body)
                .exchange().expectStatus().isForbidden();
    }
}
