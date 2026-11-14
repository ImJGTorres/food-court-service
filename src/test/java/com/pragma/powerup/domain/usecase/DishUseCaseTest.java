package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.CategoryNotFoundException;
import com.pragma.powerup.domain.exception.DishAlreadyExistsException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.CategoryModel;
import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.ICategoryPersistencePort;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishUseCaseTest {
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long CATEGORY_ID = 5L;

    @Mock
    private IDishPersistencePort dishPersistencePort;

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private ICategoryPersistencePort categoryPersistencePort;

    @InjectMocks
    private DishUseCase dishUseCase;

    private DishModel buildDish() {
        RestaurantModel restaurant = new RestaurantModel();
        restaurant.setId(RESTAURANT_ID);
        CategoryModel category = new CategoryModel();
        category.setId(CATEGORY_ID);
        DishModel dish = new DishModel();
        dish.setName("Hamburguesa");
        dish.setPrice(15000);
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        return dish;
    }

    private RestaurantModel buildRestaurantOwnedBy(Long ownerId) {
        RestaurantModel restaurant = new RestaurantModel();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setOwnerId(ownerId);
        return restaurant;
    }

    @Test
    void saveDishWithValidDataActivatesTheDishAndSavesIt() {
        DishModel dish = buildDish();
        RestaurantModel restaurant = buildRestaurantOwnedBy(OWNER_ID);
        CategoryModel category = new CategoryModel();
        category.setId(CATEGORY_ID);
        DishModel savedDish = new DishModel();
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(categoryPersistencePort.findCategoryById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(dishPersistencePort.saveDish(dish)).thenReturn(savedDish);

        DishModel result = dishUseCase.saveDish(dish, OWNER_ID);

        assertSame(savedDish, result);
        assertTrue(dish.getActive());
        assertSame(restaurant, dish.getRestaurant());
        assertSame(category, dish.getCategory());
        verify(dishPersistencePort).saveDish(dish);
    }

    @Test
    void saveDishWhenRestaurantDoesNotExistThrowsRestaurantNotFoundException() {
        DishModel dish = buildDish();
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID)).thenReturn(Optional.empty());

        RestaurantNotFoundException exception = assertThrows(
                RestaurantNotFoundException.class, () -> dishUseCase.saveDish(dish, OWNER_ID));

        assertEquals("The requested restaurant does not exist", exception.getMessage());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void saveDishWhenCallerIsNotTheRestaurantOwnerThrowsNotRestaurantOwnerException() {
        DishModel dish = buildDish();
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OTHER_OWNER_ID)));

        NotRestaurantOwnerException exception = assertThrows(
                NotRestaurantOwnerException.class, () -> dishUseCase.saveDish(dish, OWNER_ID));

        assertEquals("Only the owner of the restaurant can manage its dishes", exception.getMessage());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void saveDishWhenCategoryDoesNotExistThrowsCategoryNotFoundException() {
        DishModel dish = buildDish();
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(categoryPersistencePort.findCategoryById(CATEGORY_ID)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class, () -> dishUseCase.saveDish(dish, OWNER_ID));

        assertEquals("The requested category does not exist", exception.getMessage());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void saveDishWhenTheRestaurantAlreadyHasADishWithTheSameNameThrowsDishAlreadyExistsException() {
        DishModel dish = buildDish();
        CategoryModel category = new CategoryModel();
        category.setId(CATEGORY_ID);
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(categoryPersistencePort.findCategoryById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(dishPersistencePort.existsByNameAndRestaurantId("Hamburguesa", RESTAURANT_ID)).thenReturn(true);

        DishAlreadyExistsException exception = assertThrows(
                DishAlreadyExistsException.class, () -> dishUseCase.saveDish(dish, OWNER_ID));

        assertEquals("A dish with this name already exists in the restaurant", exception.getMessage());
        verify(dishPersistencePort, never()).saveDish(any());
    }
}
