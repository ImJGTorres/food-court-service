package com.pragma.powerup.domain.spi;

import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.PageModel;

import java.util.Optional;

public interface IDishPersistencePort {
    DishModel saveDish(DishModel dishModel);

    boolean existsByNameAndRestaurantId(String name, Long restaurantId);

    Optional<DishModel> findDishById(Long id);

    PageModel<DishModel> findActiveDishesByRestaurant(Long restaurantId, Long categoryId, int page, int size);
}
