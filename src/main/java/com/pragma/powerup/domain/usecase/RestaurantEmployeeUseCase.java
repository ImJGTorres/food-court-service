package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IRestaurantEmployeeServicePort;
import com.pragma.powerup.domain.exception.EmployeeAlreadyAssignedException;
import com.pragma.powerup.domain.exception.InvalidEmployeeException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.RestaurantEmployeeModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IRestaurantEmployeePersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.domain.spi.IUserClientPort;

public class RestaurantEmployeeUseCase implements IRestaurantEmployeeServicePort {
    private static final String RESTAURANT_NOT_FOUND_MESSAGE = "The requested restaurant does not exist";
    private static final String NOT_RESTAURANT_OWNER_MESSAGE = "Only the owner of the restaurant can manage its employees";
    private static final String EMPLOYEE_ALREADY_ASSIGNED_MESSAGE = "The employee is already assigned to a restaurant";
    private static final String INVALID_EMPLOYEE_MESSAGE = "The user is not an employee";

    private final IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IUserClientPort userClientPort;

    public RestaurantEmployeeUseCase(IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort,
                                     IRestaurantPersistencePort restaurantPersistencePort,
                                     IUserClientPort userClientPort) {
        this.restaurantEmployeePersistencePort = restaurantEmployeePersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.userClientPort = userClientPort;
    }

    @Override
    public RestaurantEmployeeModel assignEmployee(Long restaurantId, Long employeeId, Long ownerId) {
        RestaurantModel restaurant = restaurantPersistencePort.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(RESTAURANT_NOT_FOUND_MESSAGE));
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new NotRestaurantOwnerException(NOT_RESTAURANT_OWNER_MESSAGE);
        }
        if (restaurantEmployeePersistencePort.existsByEmployeeId(employeeId)) {
            throw new EmployeeAlreadyAssignedException(EMPLOYEE_ALREADY_ASSIGNED_MESSAGE);
        }
        if (!userClientPort.isEmployee(employeeId)) {
            throw new InvalidEmployeeException(INVALID_EMPLOYEE_MESSAGE);
        }
        return restaurantEmployeePersistencePort.saveRestaurantEmployee(
                new RestaurantEmployeeModel(employeeId, restaurantId));
    }
}
