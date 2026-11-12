package com.pragma.powerup.infrastructure.out.feign.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFeignResponse {
    private Long id;
    private String role;
}
