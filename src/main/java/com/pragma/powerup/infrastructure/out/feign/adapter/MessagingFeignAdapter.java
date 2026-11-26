package com.pragma.powerup.infrastructure.out.feign.adapter;

import com.pragma.powerup.domain.exception.MessagingServiceUnavailableException;
import com.pragma.powerup.domain.exception.NotificationRejectedException;
import com.pragma.powerup.domain.spi.INotificationPort;
import com.pragma.powerup.infrastructure.out.feign.client.IMessagingFeignClient;
import com.pragma.powerup.infrastructure.out.feign.dto.SmsFeignRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessagingFeignAdapter implements INotificationPort {
    private static final String NOTIFICATION_REJECTED_MESSAGE = "The client phone number cannot receive SMS";
    private static final String MESSAGING_SERVICE_UNAVAILABLE_MESSAGE = "The messaging service is not available";

    private final IMessagingFeignClient messagingFeignClient;

    @Override
    public void sendSms(String phone, String message) {
        try {
            messagingFeignClient.sendSms(new SmsFeignRequest(phone, message));
        } catch (FeignException.BadRequest exception) {
            throw new NotificationRejectedException(NOTIFICATION_REJECTED_MESSAGE);
        } catch (FeignException exception) {
            throw new MessagingServiceUnavailableException(MESSAGING_SERVICE_UNAVAILABLE_MESSAGE);
        }
    }
}
