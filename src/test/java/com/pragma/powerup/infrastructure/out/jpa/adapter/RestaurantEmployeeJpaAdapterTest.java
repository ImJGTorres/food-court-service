package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.RestaurantEmployeeModel;
import com.pragma.powerup.infrastructure.out.jpa.entity.RestaurantEmployeeEntity;
import com.pragma.powerup.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IRestaurantEmployeeEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IRestaurantEmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantEmployeeJpaAdapterTest {
    private static final Long EMPLOYEE_ID = 15L;
    private static final Long RESTAURANT_ID = 1L;

    @Mock
    private IRestaurantEmployeeRepository restaurantEmployeeRepository;

    @Mock
    private IRestaurantEmployeeEntityMapper restaurantEmployeeEntityMapper;

    @InjectMocks
    private RestaurantEmployeeJpaAdapter restaurantEmployeeJpaAdapter;

    @Test
    void saveRestaurantEmployeeConvertsSavesAndReturnsTheSavedLinkAsModel() {
        RestaurantEmployeeModel linkToSave = new RestaurantEmployeeModel(EMPLOYEE_ID, RESTAURANT_ID);
        RestaurantEmployeeEntity entityToSave = new RestaurantEmployeeEntity();
        RestaurantEmployeeEntity savedEntity = new RestaurantEmployeeEntity();
        RestaurantEmployeeModel savedLink = new RestaurantEmployeeModel(EMPLOYEE_ID, RESTAURANT_ID);
        when(restaurantEmployeeEntityMapper.toEntity(linkToSave)).thenReturn(entityToSave);
        when(restaurantEmployeeRepository.save(entityToSave)).thenReturn(savedEntity);
        when(restaurantEmployeeEntityMapper.toModel(savedEntity)).thenReturn(savedLink);

        RestaurantEmployeeModel result = restaurantEmployeeJpaAdapter.saveRestaurantEmployee(linkToSave);

        assertSame(savedLink, result);
        verify(restaurantEmployeeRepository).save(entityToSave);
    }

    @Test
    void existsByEmployeeIdAsksTheRepository() {
        when(restaurantEmployeeRepository.existsByEmployeeId(EMPLOYEE_ID)).thenReturn(true);

        assertTrue(restaurantEmployeeJpaAdapter.existsByEmployeeId(EMPLOYEE_ID));
    }

    @Test
    void findRestaurantIdByEmployeeIdReturnsTheIdOfTheLinkedRestaurant() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(RESTAURANT_ID);
        RestaurantEmployeeEntity link = new RestaurantEmployeeEntity();
        link.setEmployeeId(EMPLOYEE_ID);
        link.setRestaurant(restaurant);
        when(restaurantEmployeeRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(link));

        assertEquals(Optional.of(RESTAURANT_ID), restaurantEmployeeJpaAdapter.findRestaurantIdByEmployeeId(EMPLOYEE_ID));
    }

    @Test
    void findRestaurantIdByEmployeeIdWhenTheEmployeeIsNotLinkedReturnsEmpty() {
        when(restaurantEmployeeRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertTrue(restaurantEmployeeJpaAdapter.findRestaurantIdByEmployeeId(EMPLOYEE_ID).isEmpty());
    }
}
