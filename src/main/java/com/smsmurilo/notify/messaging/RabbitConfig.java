package com.smsmurilo.notify.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology: one exchange, a priority queue (codes/alerts jump the line) and a dead-letter queue
 * for messages that keep failing so they can be inspected and replayed.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "notifications";
    public static final String QUEUE = "notifications.dispatch";
    public static final String DLQ = "notifications.dispatch.dlq";

    @Bean
    DirectExchange notificationsExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Queue dispatchQueue() {
        return QueueBuilder.durable(QUEUE)
                .maxPriority(10)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding dispatchBinding(Queue dispatchQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(dispatchQueue).to(notificationsExchange).with(QUEUE);
    }

    @Bean
    MessageConverter jsonConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
