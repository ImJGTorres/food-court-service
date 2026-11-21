package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.InvalidRestaurantNameException;
import com.pragma.powerup.domain.exception.RestaurantAlreadyExistsException;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.domain.spi.IUserClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class RestaurantUseCaseTest {
    private static final Long OWNER_ID = 1L;
    private static final String NIT = "900111222";
    private static final String NEGATIVE_PAGE_MESSAGE = "The page number cannot be negative";
    private static final String INVALID_SIZE_MESSAGE = "The page size must be between 1 and 50";

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private IUserClientPort userClientPort;

    @InjectMocks
    private RestaurantUseCase restaurantUseCase;

    private RestaurantModel buildRestaurantNamed(String name) {
        RestaurantModel restaurant = new RestaurantModel();
        restaurant.setName(name);
        restaurant.setNit(NIT);
        restaurant.setAddress("Calle 1 # 2-3");
        restaurant.setPhone("+573001112233");
        restaurant.setLogoUrl("https://logos.com/pizza.png");
        restaurant.setOwnerId(OWNER_ID);
        return restaurant;
    }

    @Test
    void saveRestaurantWithValidDataSavesAndReturnsTheRestaurant() {
        RestaurantModel restaurant = buildRestaurantNamed("Pizza 24");
        RestaurantModel savedRestaurant = new RestaurantModel();
        when(restaurantPersistencePort.existsByNit(NIT)).thenReturn(false);
        when(userClientPort.isOwner(OWNER_ID)).thenReturn(true);
        when(restaurantPersistencePort.saveRestaurant(restaurant)).thenReturn(savedRestaurant);

        RestaurantModel result = restaurantUseCase.saveRestaurant(restaurant);

        assertSame(savedRestaurant, result);
        verify(restaurantPersistencePort).saveRestaurant(restaurant);
    }

    @Test
    void saveRestaurantWithNameMadeOnlyOfDigitsThrowsInvalidRestaurantNameException() {
        RestaurantModel restaurant = buildRestaurantNamed("12345");

        InvalidRestaurantNameException exception = assertThrows(
                InvalidRestaurantNameException.class, () -> restaurantUseCase.saveRestaurant(restaurant));

        assertEquals("The restaurant name cannot contain only numbers", exception.getMessage());
        verifyNoInteractions(restaurantPersistencePort, userClientPort);
    }

    @Test
    void saveRestaurantWithExistingNitThrowsRestaurantAlreadyExistsException() {
        RestaurantModel restaurant = buildRestaurantNamed("Pizza 24");
        when(restaurantPersistencePort.existsByNit(NIT)).thenReturn(true);

        RestaurantAlreadyExistsException exception = assertThrows(
                RestaurantAlreadyExistsException.class, () -> restaurantUseCase.saveRestaurant(restaurant));

        assertEquals("A restaurant with this NIT already exists", exception.getMessage());
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
        verifyNoInteractions(userClientPort);
    }

    @Test
    void saveRestaurantWhenUserIsNotAnOwnerThrowsInvalidOwnerException() {
        RestaurantModel restaurant = buildRestaurantNamed("Pizza 24");
        when(restaurantPersistencePort.existsByNit(NIT)).thenReturn(false);
        when(userClientPort.isOwner(OWNER_ID)).thenReturn(false);

        InvalidOwnerException exception = assertThrows(
                InvalidOwnerException.class, () -> restaurantUseCase.saveRestaurant(restaurant));

        assertEquals("The user is not an owner", exception.getMessage());
        verify(restaurantPersistencePort, never()).saveRestaurant(any());
    }

    @Test
    void listRestaurantsReturnsThePageGivenByThePersistencePort() {
        PageModel<RestaurantModel> expectedPage = new PageModel<>(
                List.of(buildRestaurantNamed("Arepas"), buildRestaurantNamed("Burger")), 0, 10, 2L, 1);
        when(restaurantPersistencePort.findAllRestaurants(0, 10)).thenReturn(expectedPage);

        PageModel<RestaurantModel> result = restaurantUseCase.listRestaurants(0, 10);

        assertSame(expectedPage, result);
        verify(restaurantPersistencePort).findAllRestaurants(0, 10);
    }

    @Test
    void listRestaurantsAcceptsTheSmallestPageSize() {
        PageModel<RestaurantModel> expectedPage = new PageModel<>(List.of(), 0, 1, 0L, 0);
        when(restaurantPersistencePort.findAllRestaurants(0, 1)).thenReturn(expectedPage);

        assertSame(expectedPage, restaurantUseCase.listRestaurants(0, 1));
    }

    @Test
    void listRestaurantsAcceptsTheBiggestPageSize() {
        PageModel<RestaurantModel> expectedPage = new PageModel<>(List.of(), 0, 50, 0L, 0);
        when(restaurantPersistencePort.findAllRestaurants(0, 50)).thenReturn(expectedPage);

        assertSame(expectedPage, restaurantUseCase.listRestaurants(0, 50));
    }

    @Test
    void listRestaurantsWithNegativePageThrowsInvalidPaginationException() {
        InvalidPaginationException exception = assertThrows(
                InvalidPaginationException.class, () -> restaurantUseCase.listRestaurants(-1, 10));

        assertEquals(NEGATIVE_PAGE_MESSAGE, exception.getMessage());
        verify(restaurantPersistencePort, never()).findAllRestaurants(anyInt(), anyInt());
    }

    @Test
    void listRestaurantsWithPageSizeZeroThrowsInvalidPaginationException() {
        InvalidPaginationException exception = assertThrows(
                InvalidPaginationException.class, () -> restaurantUseCase.listRestaurants(0, 0));

        assertEquals(INVALID_SIZE_MESSAGE, exception.getMessage());
        verify(restaurantPersistencePort, never()).findAllRestaurants(anyInt(), anyInt());
    }

    @Test
    void listRestaurantsWithPageSizeAboveTheLimitThrowsInvalidPaginationException() {
        InvalidPaginationException exception = assertThrows(
                InvalidPaginationException.class, () -> restaurantUseCase.listRestaurants(0, 51));

        assertEquals(INVALID_SIZE_MESSAGE, exception.getMessage());
        verify(restaurantPersistencePort, never()).findAllRestaurants(anyInt(), anyInt());
    }
}
