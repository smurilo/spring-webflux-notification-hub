package com.smsmurilo.notify.messaging;

import com.smsmurilo.notify.domain.Notification;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class NotificationPublisher {

    private final RabbitTemplate rabbit;

    public NotificationPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    /** RabbitTemplate is blocking, so it runs on boundedElastic to keep the event loop free. */
    public Mono<Notification> publish(Notification notification) {
        return Mono.fromCallable(() -> {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.QUEUE, notification, message -> {
                message.getMessageProperties().setPriority(notification.priority() == Notification.Priority.HIGH ? 9 : 1);
                message.getMessageProperties().setMessageId(notification.id().toString());
                return message;
            });
            return notification;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
