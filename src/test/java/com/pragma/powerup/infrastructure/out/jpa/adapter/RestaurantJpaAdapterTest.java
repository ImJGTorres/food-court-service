package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IRestaurantEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IRestaurantRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantJpaAdapterTest {

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private IRestaurantEntityMapper restaurantEntityMapper;

    @InjectMocks
    private RestaurantJpaAdapter restaurantJpaAdapter;

    @Test
    void findAllRestaurantsAsksTheRepositoryForThePageSortedByNameAscending() {
        when(restaurantRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        restaurantJpaAdapter.findAllRestaurants(2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(restaurantRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertAll(
                () -> assertEquals(2, pageable.getPageNumber()),
                () -> assertEquals(5, pageable.getPageSize()),
                () -> assertEquals(Sort.by("name").ascending(), pageable.getSort())
        );
    }

    @Test
    void findAllRestaurantsConvertsTheSpringPageIntoThePageModel() {
        RestaurantEntity firstEntity = new RestaurantEntity();
        RestaurantEntity secondEntity = new RestaurantEntity();
        RestaurantModel firstModel = new RestaurantModel();
        RestaurantModel secondModel = new RestaurantModel();
        Page<RestaurantEntity> springPage = new PageImpl<>(
                List.of(firstEntity, secondEntity), PageRequest.of(1, 2), 5);
        when(restaurantRepository.findAll(any(Pageable.class))).thenReturn(springPage);
        when(restaurantEntityMapper.toModel(firstEntity)).thenReturn(firstModel);
        when(restaurantEntityMapper.toModel(secondEntity)).thenReturn(secondModel);

        PageModel<RestaurantModel> result = restaurantJpaAdapter.findAllRestaurants(1, 2);

        assertAll(
                () -> assertEquals(2, result.getContent().size()),
                () -> assertSame(firstModel, result.getContent().get(0)),
                () -> assertSame(secondModel, result.getContent().get(1)),
                () -> assertEquals(1, result.getPage()),
                () -> assertEquals(2, result.getSize()),
                () -> assertEquals(5L, result.getTotalElements()),
                () -> assertEquals(3, result.getTotalPages())
        );
    }

    @Test
    void findAllRestaurantsWhenThereAreNoRestaurantsReturnsAnEmptyPage() {
        Page<RestaurantEntity> springPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(restaurantRepository.findAll(any(Pageable.class))).thenReturn(springPage);

        PageModel<RestaurantModel> result = restaurantJpaAdapter.findAllRestaurants(0, 10);

        assertAll(
                () -> assertTrue(result.getContent().isEmpty()),
                () -> assertEquals(0L, result.getTotalElements()),
                () -> assertEquals(0, result.getTotalPages())
        );
    }
}
