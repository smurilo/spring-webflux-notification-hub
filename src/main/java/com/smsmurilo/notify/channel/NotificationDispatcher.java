package com.smsmurilo.notify.channel;

import com.smsmurilo.notify.domain.DeliveryStatus;
import com.smsmurilo.notify.domain.Notification;
import com.smsmurilo.notify.domain.TemplateRenderer;
import com.smsmurilo.notify.messaging.RabbitConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatcher {

    private final Map<Notification.Channel, ChannelSender> senders = new EnumMap<>(Notification.Channel.class);
    private final TemplateRenderer renderer;
    private final MeterRegistry metrics;
    /** Hot stream of delivery updates, exposed to clients through Server-Sent Events. */
    private final Sinks.Many<DeliveryStatus> statusSink = Sinks.many().multicast().directBestEffort();

    public NotificationDispatcher(List<ChannelSender> senders, TemplateRenderer renderer, MeterRegistry metrics) {
        senders.forEach(s -> this.senders.put(s.channel(), s));
        this.renderer = renderer;
        this.metrics = metrics;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void dispatch(Notification n) {
        try {
            var content = renderer.render(n.template(), n.variables());
            senders.get(n.channel()).send(n.recipient(), content);
            emit(n, DeliveryStatus.State.SENT, null);
            metrics.counter("notifications.sent", "channel", n.channel().name(), "tenant", n.tenant()).increment();
        } catch (IllegalArgumentException invalid) {
            // Permanent error: retrying will never succeed, so route straight to the DLQ.
            emit(n, DeliveryStatus.State.FAILED, invalid.getMessage());
            metrics.counter("notifications.failed", "channel", n.channel().name()).increment();
            throw new AmqpRejectAndDontRequeueException(invalid);
        }
    }

    public void queued(Notification n) {
        emit(n, DeliveryStatus.State.QUEUED, null);
    }

    public Flux<DeliveryStatus> statusStream(String tenant) {
        return statusSink.asFlux().filter(s -> s.tenant().equals(tenant));
    }

    private void emit(Notification n, DeliveryStatus.State state, String detail) {
        statusSink.tryEmitNext(new DeliveryStatus(n.id(), n.tenant(), state, detail, Instant.now()));
    }
}
