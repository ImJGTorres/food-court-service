package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.CategoryNotFoundException;
import com.pragma.powerup.domain.exception.DishAlreadyExistsException;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.CategoryModel;
import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.ICategoryPersistencePort;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishUseCaseTest {
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Long RESTAURANT_ID = 10L;
    private static final Long CATEGORY_ID = 5L;
    private static final Long DISH_ID = 7L;

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

    private DishModel buildExistingDish(Long ownerId) {
        DishModel dish = new DishModel();
        dish.setId(DISH_ID);
        dish.setName("Hamburguesa");
        dish.setPrice(15000);
        dish.setDescription("Con queso");
        dish.setActive(true);
        dish.setRestaurant(buildRestaurantOwnedBy(ownerId));
        return dish;
    }

    private DishModel buildChanges() {
        DishModel changes = new DishModel();
        changes.setPrice(18000);
        changes.setDescription("Con queso y tocineta");
        return changes;
    }

    @Test
    void updateDishWithValidDataChangesOnlyPriceAndDescriptionAndSavesIt() {
        DishModel existingDish = buildExistingDish(OWNER_ID);
        DishModel savedDish = new DishModel();
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.of(existingDish));
        when(dishPersistencePort.saveDish(existingDish)).thenReturn(savedDish);

        DishModel result = dishUseCase.updateDish(DISH_ID, buildChanges(), OWNER_ID);

        assertSame(savedDish, result);
        assertEquals(18000, existingDish.getPrice());
        assertEquals("Con queso y tocineta", existingDish.getDescription());
        assertEquals("Hamburguesa", existingDish.getName());
        assertTrue(existingDish.getActive());
        verify(dishPersistencePort).saveDish(existingDish);
    }

    @Test
    void updateDishWhenDishDoesNotExistThrowsDishNotFoundException() {
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.empty());

        DishNotFoundException exception = assertThrows(
                DishNotFoundException.class, () -> dishUseCase.updateDish(DISH_ID, buildChanges(), OWNER_ID));

        assertEquals("The requested dish does not exist", exception.getMessage());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void updateDishWhenCallerIsNotTheRestaurantOwnerThrowsNotRestaurantOwnerException() {
        DishModel existingDish = buildExistingDish(OTHER_OWNER_ID);
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.of(existingDish));

        NotRestaurantOwnerException exception = assertThrows(
                NotRestaurantOwnerException.class, () -> dishUseCase.updateDish(DISH_ID, buildChanges(), OWNER_ID));

        assertEquals("Only the owner of the restaurant can manage its dishes", exception.getMessage());
        assertEquals(15000, existingDish.getPrice());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void updateDishStatusToInactiveDisablesTheDishAndChangesNothingElse() {
        DishModel existingDish = buildExistingDish(OWNER_ID);
        DishModel savedDish = new DishModel();
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.of(existingDish));
        when(dishPersistencePort.saveDish(existingDish)).thenReturn(savedDish);

        DishModel result = dishUseCase.updateDishStatus(DISH_ID, false, OWNER_ID);

        assertSame(savedDish, result);
        assertFalse(existingDish.getActive());
        assertEquals(15000, existingDish.getPrice());
        assertEquals("Con queso", existingDish.getDescription());
        assertEquals("Hamburguesa", existingDish.getName());
        verify(dishPersistencePort).saveDish(existingDish);
    }

    @Test
    void updateDishStatusToActiveEnablesADisabledDish() {
        DishModel existingDish = buildExistingDish(OWNER_ID);
        existingDish.setActive(false);
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.of(existingDish));
        when(dishPersistencePort.saveDish(existingDish)).thenReturn(existingDish);

        DishModel result = dishUseCase.updateDishStatus(DISH_ID, true, OWNER_ID);

        assertTrue(result.getActive());
        verify(dishPersistencePort).saveDish(existingDish);
    }

    @Test
    void updateDishStatusWithTheCurrentStatusIsAcceptedAndKeepsTheDishUnchanged() {
        DishModel existingDish = buildExistingDish(OWNER_ID);
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.of(existingDish));
        when(dishPersistencePort.saveDish(existingDish)).thenReturn(existingDish);

        DishModel result = dishUseCase.updateDishStatus(DISH_ID, true, OWNER_ID);

        assertTrue(result.getActive());
        verify(dishPersistencePort).saveDish(existingDish);
    }

    @Test
    void updateDishStatusWhenDishDoesNotExistThrowsDishNotFoundException() {
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.empty());

        DishNotFoundException exception = assertThrows(
                DishNotFoundException.class, () -> dishUseCase.updateDishStatus(DISH_ID, false, OWNER_ID));

        assertEquals("The requested dish does not exist", exception.getMessage());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void updateDishStatusWhenCallerIsNotTheRestaurantOwnerThrowsNotRestaurantOwnerException() {
        DishModel existingDish = buildExistingDish(OTHER_OWNER_ID);
        when(dishPersistencePort.findDishById(DISH_ID)).thenReturn(Optional.of(existingDish));

        NotRestaurantOwnerException exception = assertThrows(
                NotRestaurantOwnerException.class, () -> dishUseCase.updateDishStatus(DISH_ID, false, OWNER_ID));

        assertEquals("Only the owner of the restaurant can manage its dishes", exception.getMessage());
        assertTrue(existingDish.getActive());
        verify(dishPersistencePort, never()).saveDish(any());
    }

    @Test
    void listDishesWithoutCategoryReturnsThePageOfThePortAndDoesNotLookUpCategories() {
        PageModel<DishModel> expectedPage = new PageModel<>(List.of(buildDish()), 0, 10, 1L, 1);
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(dishPersistencePort.findActiveDishesByRestaurant(RESTAURANT_ID, null, 0, 10)).thenReturn(expectedPage);

        PageModel<DishModel> result = dishUseCase.listDishes(RESTAURANT_ID, null, 0, 10);

        assertSame(expectedPage, result);
        verifyNoInteractions(categoryPersistencePort);
    }

    @Test
    void listDishesWithCategoryChecksThatTheCategoryExistsAndFiltersByIt() {
        PageModel<DishModel> expectedPage = new PageModel<>(List.of(), 0, 10, 0L, 0);
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(categoryPersistencePort.findCategoryById(CATEGORY_ID)).thenReturn(Optional.of(new CategoryModel()));
        when(dishPersistencePort.findActiveDishesByRestaurant(RESTAURANT_ID, CATEGORY_ID, 0, 10))
                .thenReturn(expectedPage);

        PageModel<DishModel> result = dishUseCase.listDishes(RESTAURANT_ID, CATEGORY_ID, 0, 10);

        assertSame(expectedPage, result);
        verify(categoryPersistencePort).findCategoryById(CATEGORY_ID);
    }

    @Test
    void listDishesOfAMissingRestaurantThrowsRestaurantNotFoundException() {
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID)).thenReturn(Optional.empty());

        RestaurantNotFoundException exception = assertThrows(RestaurantNotFoundException.class,
                () -> dishUseCase.listDishes(RESTAURANT_ID, null, 0, 10));

        assertEquals("The requested restaurant does not exist", exception.getMessage());
        verify(dishPersistencePort, never()).findActiveDishesByRestaurant(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    void listDishesWithAMissingCategoryThrowsCategoryNotFoundException() {
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(categoryPersistencePort.findCategoryById(CATEGORY_ID)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class,
                () -> dishUseCase.listDishes(RESTAURANT_ID, CATEGORY_ID, 0, 10));

        assertEquals("The requested category does not exist", exception.getMessage());
        verify(dishPersistencePort, never()).findActiveDishesByRestaurant(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    void listDishesWithInvalidPaginationThrowsBeforeTouchingAnyPort() {
        InvalidPaginationException exception = assertThrows(InvalidPaginationException.class,
                () -> dishUseCase.listDishes(RESTAURANT_ID, null, 0, 51));

        assertEquals("The page size must be between 1 and 50", exception.getMessage());
        verifyNoInteractions(restaurantPersistencePort, categoryPersistencePort, dishPersistencePort);
    }
}
