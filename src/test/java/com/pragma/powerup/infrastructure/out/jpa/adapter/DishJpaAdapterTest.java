package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IDishEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IDishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishJpaAdapterTest {
    private static final Long RESTAURANT_ID = 1L;
    private static final Long CATEGORY_ID = 2L;

    @Mock
    private IDishRepository dishRepository;

    @Mock
    private IDishEntityMapper dishEntityMapper;

    @InjectMocks
    private DishJpaAdapter dishJpaAdapter;

    @Test
    void findActiveDishesWithoutCategoryAsksForAllActiveDishesOfTheRestaurantSortedByName() {
        when(dishRepository.findByRestaurantIdAndActiveTrue(eq(RESTAURANT_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        dishJpaAdapter.findActiveDishesByRestaurant(RESTAURANT_ID, null, 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dishRepository).findByRestaurantIdAndActiveTrue(eq(RESTAURANT_ID), pageableCaptor.capture());
        verify(dishRepository, never())
                .findByRestaurantIdAndCategoryIdAndActiveTrue(anyLong(), anyLong(), any(Pageable.class));
        Pageable pageable = pageableCaptor.getValue();
        assertAll(
                () -> assertEquals(2, pageable.getPageNumber()),
                () -> assertEquals(5, pageable.getPageSize()),
                () -> assertEquals(Sort.by("name").ascending(), pageable.getSort())
        );
    }

    @Test
    void findActiveDishesWithCategoryAsksOnlyForTheDishesOfThatCategory() {
        when(dishRepository.findByRestaurantIdAndCategoryIdAndActiveTrue(
                eq(RESTAURANT_ID), eq(CATEGORY_ID), any(Pageable.class))).thenReturn(Page.empty());

        dishJpaAdapter.findActiveDishesByRestaurant(RESTAURANT_ID, CATEGORY_ID, 0, 10);

        verify(dishRepository).findByRestaurantIdAndCategoryIdAndActiveTrue(
                eq(RESTAURANT_ID), eq(CATEGORY_ID), any(Pageable.class));
        verify(dishRepository, never()).findByRestaurantIdAndActiveTrue(anyLong(), any(Pageable.class));
    }

    @Test
    void findActiveDishesConvertsTheSpringPageIntoThePageModel() {
        DishEntity firstEntity = new DishEntity();
        DishEntity secondEntity = new DishEntity();
        DishModel firstModel = new DishModel();
        DishModel secondModel = new DishModel();
        Page<DishEntity> springPage = new PageImpl<>(List.of(firstEntity, secondEntity), PageRequest.of(1, 2), 5);
        when(dishRepository.findByRestaurantIdAndActiveTrue(eq(RESTAURANT_ID), any(Pageable.class)))
                .thenReturn(springPage);
        when(dishEntityMapper.toModel(firstEntity)).thenReturn(firstModel);
        when(dishEntityMapper.toModel(secondEntity)).thenReturn(secondModel);

        PageModel<DishModel> result = dishJpaAdapter.findActiveDishesByRestaurant(RESTAURANT_ID, null, 1, 2);

        assertAll(
                () -> assertSame(firstModel, result.getContent().get(0)),
                () -> assertSame(secondModel, result.getContent().get(1)),
                () -> assertEquals(1, result.getPage()),
                () -> assertEquals(2, result.getSize()),
                () -> assertEquals(5L, result.getTotalElements()),
                () -> assertEquals(3, result.getTotalPages())
        );
    }
}
