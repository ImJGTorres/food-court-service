package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.EmployeeAlreadyAssignedException;
import com.pragma.powerup.domain.exception.InvalidEmployeeException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.RestaurantEmployeeModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IRestaurantEmployeePersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.domain.spi.IUserClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantEmployeeUseCaseTest {
    private static final Long RESTAURANT_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_OWNER_ID = 11L;
    private static final Long EMPLOYEE_ID = 15L;

    @Mock
    private IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort;

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private IUserClientPort userClientPort;

    @InjectMocks
    private RestaurantEmployeeUseCase restaurantEmployeeUseCase;

    private RestaurantModel buildRestaurantOwnedBy(Long ownerId) {
        RestaurantModel restaurant = new RestaurantModel();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setOwnerId(ownerId);
        return restaurant;
    }

    @Test
    void assignEmployeeToTheOwnersRestaurantSavesTheLink() {
        RestaurantEmployeeModel savedLink = new RestaurantEmployeeModel(EMPLOYEE_ID, RESTAURANT_ID);
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(restaurantEmployeePersistencePort.existsByEmployeeId(EMPLOYEE_ID)).thenReturn(false);
        when(userClientPort.isEmployee(EMPLOYEE_ID)).thenReturn(true);
        when(restaurantEmployeePersistencePort.saveRestaurantEmployee(any(RestaurantEmployeeModel.class)))
                .thenReturn(savedLink);

        RestaurantEmployeeModel result = restaurantEmployeeUseCase.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID);

        assertSame(savedLink, result);
        ArgumentCaptor<RestaurantEmployeeModel> linkCaptor = ArgumentCaptor.forClass(RestaurantEmployeeModel.class);
        verify(restaurantEmployeePersistencePort).saveRestaurantEmployee(linkCaptor.capture());
        assertAll(
                () -> assertEquals(EMPLOYEE_ID, linkCaptor.getValue().getEmployeeId()),
                () -> assertEquals(RESTAURANT_ID, linkCaptor.getValue().getRestaurantId())
        );
    }

    @Test
    void assignEmployeeToAMissingRestaurantThrowsRestaurantNotFoundException() {
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID)).thenReturn(Optional.empty());

        RestaurantNotFoundException exception = assertThrows(RestaurantNotFoundException.class,
                () -> restaurantEmployeeUseCase.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID));

        assertEquals("The requested restaurant does not exist", exception.getMessage());
        verifyNoInteractions(restaurantEmployeePersistencePort, userClientPort);
    }

    @Test
    void assignEmployeeToARestaurantOfAnotherOwnerThrowsNotRestaurantOwnerException() {
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OTHER_OWNER_ID)));

        NotRestaurantOwnerException exception = assertThrows(NotRestaurantOwnerException.class,
                () -> restaurantEmployeeUseCase.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID));

        assertEquals("Only the owner of the restaurant can manage its employees", exception.getMessage());
        verifyNoInteractions(restaurantEmployeePersistencePort, userClientPort);
    }

    @Test
    void assignAnEmployeeThatAlreadyWorksInARestaurantThrowsEmployeeAlreadyAssignedException() {
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(restaurantEmployeePersistencePort.existsByEmployeeId(EMPLOYEE_ID)).thenReturn(true);

        EmployeeAlreadyAssignedException exception = assertThrows(EmployeeAlreadyAssignedException.class,
                () -> restaurantEmployeeUseCase.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID));

        assertEquals("The employee is already assigned to a restaurant", exception.getMessage());
        verify(restaurantEmployeePersistencePort, never()).saveRestaurantEmployee(any());
        verifyNoInteractions(userClientPort);
    }

    @Test
    void assignAUserThatIsNotAnEmployeeThrowsInvalidEmployeeException() {
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurantOwnedBy(OWNER_ID)));
        when(restaurantEmployeePersistencePort.existsByEmployeeId(EMPLOYEE_ID)).thenReturn(false);
        when(userClientPort.isEmployee(EMPLOYEE_ID)).thenReturn(false);

        InvalidEmployeeException exception = assertThrows(InvalidEmployeeException.class,
                () -> restaurantEmployeeUseCase.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID));

        assertEquals("The user is not an employee", exception.getMessage());
        verify(restaurantEmployeePersistencePort, never()).saveRestaurantEmployee(any());
    }
}
