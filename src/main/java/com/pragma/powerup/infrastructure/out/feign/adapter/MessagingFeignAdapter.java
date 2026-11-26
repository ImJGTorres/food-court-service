package com.pragma.powerup.infrastructure.out.feign.adapter;

import com.pragma.powerup.domain.spi.INotificationPort;

public class MessagingFeignAdapter implements INotificationPort {

    @Override
    public void sendSms(String phone, String message) {
        throw new UnsupportedOperationException("Implemented in step K2");
    }
}
