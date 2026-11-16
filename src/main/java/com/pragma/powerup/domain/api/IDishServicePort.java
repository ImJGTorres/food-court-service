package com.pragma.powerup.domain.api;

import com.pragma.powerup.domain.model.DishModel;

public interface IDishServicePort {
    DishModel saveDish(DishModel dishModel, Long ownerId);

    DishModel updateDish(Long dishId, DishModel dishModel, Long ownerId);
}
