package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IRestaurantServicePort;
import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.exception.InvalidRestaurantNameException;
import com.pragma.powerup.domain.exception.RestaurantAlreadyExistsException;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.domain.spi.IUserClientPort;

public class RestaurantUseCase implements IRestaurantServicePort {
    private static final String ONLY_DIGITS_REGEX = "\\d+";
    private static final String NAME_ONLY_DIGITS_MESSAGE = "The restaurant name cannot contain only numbers";
    private static final String NIT_EXISTS_MESSAGE = "A restaurant with this NIT already exists";
    private static final String INVALID_OWNER_MESSAGE = "The user is not an owner";

    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IUserClientPort userClientPort;

    public RestaurantUseCase(IRestaurantPersistencePort restaurantPersistencePort, IUserClientPort userClientPort) {
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.userClientPort = userClientPort;
    }

    @Override
    public RestaurantModel saveRestaurant(RestaurantModel restaurantModel) {
        validateName(restaurantModel.getName());
        validateUniqueNit(restaurantModel.getNit());
        validateOwner(restaurantModel.getOwnerId());
        return restaurantPersistencePort.saveRestaurant(restaurantModel);
    }

    private void validateName(String name) {
        if (name.matches(ONLY_DIGITS_REGEX)) {
            throw new InvalidRestaurantNameException(NAME_ONLY_DIGITS_MESSAGE);
        }
    }

    private void validateUniqueNit(String nit) {
        if (restaurantPersistencePort.existsByNit(nit)) {
            throw new RestaurantAlreadyExistsException(NIT_EXISTS_MESSAGE);
        }
    }

    private void validateOwner(Long ownerId) {
        if (!userClientPort.isOwner(ownerId)) {
            throw new InvalidOwnerException(INVALID_OWNER_MESSAGE);
        }
    }
}
