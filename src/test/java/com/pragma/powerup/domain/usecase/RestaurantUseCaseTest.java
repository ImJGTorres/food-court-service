package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.exception.InvalidRestaurantNameException;
import com.pragma.powerup.domain.exception.RestaurantAlreadyExistsException;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.domain.spi.IUserClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantUseCaseTest {
    private static final Long OWNER_ID = 1L;
    private static final String NIT = "900111222";

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
}
