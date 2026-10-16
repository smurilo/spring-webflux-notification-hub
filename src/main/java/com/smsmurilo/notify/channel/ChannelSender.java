package com.smsmurilo.notify.channel;

import com.smsmurilo.notify.domain.Notification;

public interface ChannelSender {

    Notification.Channel channel();

    void send(String recipient, String content);
}
