package com.smsmurilo.notify.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/** Fixed-window limiter in Redis: shared by every instance, so the limit holds when the service scales out. */
@Component
public class TenantRateLimiter {

    private final ReactiveStringRedisTemplate redis;
    private final long limitPerMinute;

    public TenantRateLimiter(ReactiveStringRedisTemplate redis, @Value("${notify.rate-limit-per-minute:120}") long limitPerMinute) {
        this.redis = redis;
        this.limitPerMinute = limitPerMinute;
    }

    public Mono<Boolean> tryAcquire(String tenant) {
        var key = "rl:%s:%d".formatted(tenant, Instant.now().getEpochSecond() / 60);
        return redis.opsForValue().increment(key)
                .flatMap(count -> count == 1
                        ? redis.expire(key, Duration.ofMinutes(2)).thenReturn(count)
                        : Mono.just(count))
                .map(count -> count <= limitPerMinute);
    }
}
