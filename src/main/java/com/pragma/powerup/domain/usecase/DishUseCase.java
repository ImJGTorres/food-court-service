package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IDishServicePort;
import com.pragma.powerup.domain.exception.CategoryNotFoundException;
import com.pragma.powerup.domain.exception.DishAlreadyExistsException;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.CategoryModel;
import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.ICategoryPersistencePort;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;


public class DishUseCase implements IDishServicePort {
    private static final String RESTAURANT_NOT_FOUND_MESSAGE = "The requested restaurant does not exist";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "The requested category does not exist";
    private static final String NOT_RESTAURANT_OWNER_MESSAGE = "Only the owner of the restaurant can manage its dishes";
    private static final String DISH_ALREADY_EXISTS_MESSAGE = "A dish with this name already exists in the restaurant";
    private static final String DISH_NOT_FOUND_MESSAGE = "The requested dish does not exist";

    private final IDishPersistencePort dishPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final ICategoryPersistencePort categoryPersistencePort;

    public DishUseCase(IDishPersistencePort dishPersistencePort,
                       IRestaurantPersistencePort restaurantPersistencePort,
                       ICategoryPersistencePort categoryPersistencePort) {
        this.dishPersistencePort = dishPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.categoryPersistencePort = categoryPersistencePort;
    }

    @Override
    public DishModel saveDish(DishModel dishModel, Long ownerId) {
        RestaurantModel restaurant = findRestaurant(dishModel.getRestaurant().getId());
        validateRestaurantOwner(restaurant, ownerId);
        dishModel.setRestaurant(restaurant);
        dishModel.setCategory(findCategory(dishModel.getCategory().getId()));
        dishModel.setActive(true);
        if (dishPersistencePort.existsByNameAndRestaurantId(dishModel.getName(), restaurant.getId())) {
            throw new DishAlreadyExistsException(DISH_ALREADY_EXISTS_MESSAGE);
        }
        return dishPersistencePort.saveDish(dishModel);
    }

    @Override
    public DishModel updateDish(Long dishId, DishModel dishModel, Long ownerId) {
        DishModel dish = findDish(dishId);
        validateRestaurantOwner(dish.getRestaurant(), ownerId);
        dish.setPrice(dishModel.getPrice());
        dish.setDescription(dishModel.getDescription());
        return dishPersistencePort.saveDish(dish);
    }

    private RestaurantModel findRestaurant(Long restaurantId) {
        return restaurantPersistencePort.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(RESTAURANT_NOT_FOUND_MESSAGE));
    }

    private void validateRestaurantOwner(RestaurantModel restaurant, Long ownerId) {
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new NotRestaurantOwnerException(NOT_RESTAURANT_OWNER_MESSAGE);
        }
    }

    private CategoryModel findCategory(Long categoryId) {
        return categoryPersistencePort.findCategoryById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));
    }

    private DishModel findDish(Long dishId) {
        return dishPersistencePort.findDishById(dishId).orElseThrow(() -> new DishNotFoundException(DISH_NOT_FOUND_MESSAGE));
    }
}
