package com.pragma.powerup.domain.spi;

public interface INotificationPort {
    void sendSms(String phone, String message);
}
