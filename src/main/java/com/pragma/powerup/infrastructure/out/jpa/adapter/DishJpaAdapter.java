package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IDishEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IDishRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class DishJpaAdapter implements IDishPersistencePort {
    private final IDishRepository dishRepository;
    private final IDishEntityMapper dishEntityMapper;

    @Override
    public DishModel saveDish(DishModel dishModel) {
        DishEntity dishEntity = dishEntityMapper.toEntity(dishModel);
        return dishEntityMapper.toModel(dishRepository.save(dishEntity));
    }

    @Override
    public boolean existsByNameAndRestaurantId(String name, Long restaurantId) {
        return dishRepository.existsByNameIgnoreCaseAndRestaurantId(name, restaurantId);
    }

    @Override
    public Optional<DishModel> findDishById(Long id) {
        return dishRepository.findById(id).map(dishEntityMapper::toModel);
    }


}
