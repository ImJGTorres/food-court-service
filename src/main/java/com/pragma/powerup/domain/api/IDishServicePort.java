package com.pragma.powerup.domain.api;

import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.PageModel;

public interface IDishServicePort {
    DishModel saveDish(DishModel dishModel, Long ownerId);

    DishModel updateDish(Long dishId, DishModel dishModel, Long ownerId);

    DishModel updateDishStatus(Long dishId, Boolean active, Long ownerId);

    PageModel<DishModel> listDishes(Long restaurantId, Long categoryId, int page, int size);
}
