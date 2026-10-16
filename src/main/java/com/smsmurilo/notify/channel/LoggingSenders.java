package com.smsmurilo.notify.channel;

import com.smsmurilo.notify.domain.Notification.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Simulated providers (SMS gateway, SMTP, FCM/APNs). Swap a bean to integrate a real provider. */
@Configuration
class LoggingSenders {

    private static final Logger log = LoggerFactory.getLogger(LoggingSenders.class);

    private static ChannelSender sender(Channel channel) {
        return new ChannelSender() {
            public Channel channel() { return channel; }
            public void send(String recipient, String content) {
                log.info("[{}] -> {}: {}", channel, recipient, content);
            }
        };
    }

    @Bean ChannelSender smsSender() { return sender(Channel.SMS); }
    @Bean ChannelSender emailSender() { return sender(Channel.EMAIL); }
    @Bean ChannelSender pushSender() { return sender(Channel.PUSH); }
}
