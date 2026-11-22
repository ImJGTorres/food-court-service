package com.pragma.powerup.domain.api;

import com.pragma.powerup.domain.model.RestaurantEmployeeModel;

public interface IRestaurantEmployeeServicePort {
    RestaurantEmployeeModel assignEmployee(Long restaurantId, Long employeeId, Long ownerId);
}
