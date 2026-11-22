package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IDishEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IDishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

@RequiredArgsConstructor
public class DishJpaAdapter implements IDishPersistencePort {
    private static final String NAME_FIELD = "name";

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

    @Override
    public PageModel<DishModel> findActiveDishesByRestaurant(Long restaurantId, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(NAME_FIELD).ascending());
        Page<DishEntity> dishPage = categoryId == null
                ? dishRepository.findByRestaurantIdAndActiveTrue(restaurantId, pageable)
                : dishRepository.findByRestaurantIdAndCategoryIdAndActiveTrue(restaurantId, categoryId, pageable);
        return PageModelConverter.toPageModel(dishPage, dishEntityMapper::toModel);
    }


}
