package com.pragma.powerup.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderModel {
    private Long id;
    private Long clientId;
    private RestaurantModel restaurant;
    private LocalDateTime date;
    private OrderStatus status;
    private Long employeeId;
    private List<OrderDishModel> dishes;
}
