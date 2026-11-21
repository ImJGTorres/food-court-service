package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IRestaurantEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IRestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RestaurantJpaAdapter implements IRestaurantPersistencePort {
    private static final String NAME_FIELD = "name";

    private final IRestaurantRepository restaurantRepository;
    private final IRestaurantEntityMapper restaurantEntityMapper;

    @Override
    public Optional<RestaurantModel> findRestaurantById(Long id) {
        return restaurantRepository.findById(id).map(restaurantEntityMapper::toModel);
    }

    @Override
    public PageModel<RestaurantModel> findAllRestaurants(int page, int size) {
        Page<RestaurantEntity> restaurantPage = restaurantRepository
                .findAll(PageRequest.of(page, size, Sort.by(NAME_FIELD).ascending()));
        List<RestaurantModel> restaurants = restaurantPage
                .getContent()
                .stream()
                .map(restaurantEntityMapper::toModel)
                .toList();
        return new PageModel<>(restaurants,
                restaurantPage.getNumber(),
                restaurantPage.getSize(),
                restaurantPage.getTotalElements(),
                restaurantPage.getTotalPages());
    }

    @Override
    public RestaurantModel saveRestaurant(RestaurantModel restaurantModel) {
        RestaurantEntity restaurantEntity = restaurantRepository.save(restaurantEntityMapper.toEntity(restaurantModel));
        return restaurantEntityMapper.toModel(restaurantEntity);
    }

    @Override
    public boolean existsByNit(String nit) {
        return restaurantRepository.existsByNit(nit);
    }
}
