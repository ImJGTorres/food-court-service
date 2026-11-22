package com.pragma.powerup.domain.spi;

import com.pragma.powerup.domain.model.RestaurantEmployeeModel;

import java.util.Optional;

public interface IRestaurantEmployeePersistencePort {
    RestaurantEmployeeModel saveRestaurantEmployee(RestaurantEmployeeModel restaurantEmployeeModel);

    boolean existsByEmployeeId(Long employeeId);

    Optional<Long> findRestaurantIdByEmployeeId(Long employeeId);
}
