package com.pragma.powerup.domain.api;

import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;

public interface IRestaurantServicePort {
    RestaurantModel saveRestaurant(RestaurantModel restaurant);

    PageModel<RestaurantModel> listRestaurants(int page, int size);
}
