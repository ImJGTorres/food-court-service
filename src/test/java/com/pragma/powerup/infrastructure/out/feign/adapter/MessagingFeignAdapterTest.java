package com.pragma.powerup.infrastructure.out.feign.adapter;

import com.pragma.powerup.domain.exception.MessagingServiceUnavailableException;
import com.pragma.powerup.domain.exception.NotificationRejectedException;
import com.pragma.powerup.infrastructure.out.feign.client.IMessagingFeignClient;
import com.pragma.powerup.infrastructure.out.feign.dto.SmsFeignRequest;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessagingFeignAdapterTest {
    private static final String PHONE = "+573005698325";
    private static final String MESSAGE = "Pedido #1 listo en Arepas. Tu PIN de seguridad es 482913.";

    @Mock
    private IMessagingFeignClient messagingFeignClient;

    @InjectMocks
    private MessagingFeignAdapter messagingFeignAdapter;

    private Request buildRequest() {
        return Request.create(Request.HttpMethod.POST, "/api/v1/sms",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void sendSmsHandsThePhoneAndTheMessageToTheMessagingService() {
        messagingFeignAdapter.sendSms(PHONE, MESSAGE);

        ArgumentCaptor<SmsFeignRequest> requestCaptor = ArgumentCaptor.forClass(SmsFeignRequest.class);
        verify(messagingFeignClient).sendSms(requestCaptor.capture());
        assertAll(
                () -> assertEquals(PHONE, requestCaptor.getValue().getPhone()),
                () -> assertEquals(MESSAGE, requestCaptor.getValue().getMessage())
        );
    }

    @Test
    void sendSmsWhenTheMessagingServiceRejectsTheDataThrowsNotificationRejectedException() {
        doThrow(new FeignException.BadRequest("bad request", buildRequest(), null, Collections.emptyMap()))
                .when(messagingFeignClient).sendSms(any(SmsFeignRequest.class));

        NotificationRejectedException exception = assertThrows(NotificationRejectedException.class,
                () -> messagingFeignAdapter.sendSms(PHONE, MESSAGE));

        assertEquals("The client phone number cannot receive SMS", exception.getMessage());
    }

    @Test
    void sendSmsWhenTheMessagingServiceFailsThrowsMessagingServiceUnavailableException() {
        doThrow(new FeignException.InternalServerError("error", buildRequest(), null, Collections.emptyMap()))
                .when(messagingFeignClient).sendSms(any(SmsFeignRequest.class));

        MessagingServiceUnavailableException exception = assertThrows(MessagingServiceUnavailableException.class,
                () -> messagingFeignAdapter.sendSms(PHONE, MESSAGE));

        assertEquals("The messaging service is not available", exception.getMessage());
    }

    @Test
    void sendSmsWhenTheMessagingServiceDoesNotAcceptTheTokenThrowsMessagingServiceUnavailableException() {
        doThrow(new FeignException.Forbidden("forbidden", buildRequest(), null, Collections.emptyMap()))
                .when(messagingFeignClient).sendSms(any(SmsFeignRequest.class));

        assertThrows(MessagingServiceUnavailableException.class, () -> messagingFeignAdapter.sendSms(PHONE, MESSAGE));
    }
}
