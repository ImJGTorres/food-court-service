package com.pragma.powerup.infrastructure.out.feign.client;

import com.pragma.powerup.infrastructure.out.feign.dto.UserFeignResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users-service", url = "${users-service.url}")
public interface IUserFeignClient {

    @GetMapping("/api/v1/users/{id}")
    UserFeignResponse getUserById(@PathVariable("id") Long id);
}
