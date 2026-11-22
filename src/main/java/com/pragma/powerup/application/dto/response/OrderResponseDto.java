package com.pragma.powerup.application.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderResponseDto {
    private Long id;
    private Long clientId;
    private Long restaurantId;
    private LocalDateTime date;
    private String status;
    private Long employeeId;
    private List<OrderDishResponseDto> dishes;
}
