package com.pragma.powerup.infrastructure.out.feign.client;

import com.pragma.powerup.infrastructure.out.feign.dto.SmsFeignRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "messaging-service", url = "${messaging-service.url}")
public interface IMessagingFeignClient {

    @PostMapping("/api/v1/sms")
    void sendSms(@RequestBody SmsFeignRequest smsFeignRequest);
}
