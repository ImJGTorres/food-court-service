package com.pragma.powerup.domain.spi;

import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;

import java.util.Optional;

public interface IRestaurantPersistencePort {
    RestaurantModel saveRestaurant(RestaurantModel restaurant);

    boolean existsByNit(String nit);

    Optional<RestaurantModel> findRestaurantById(Long id);

    PageModel<RestaurantModel> findAllRestaurants(int page, int size);
}
