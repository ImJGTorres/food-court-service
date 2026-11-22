package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.DishNotFromRestaurantException;
import com.pragma.powerup.domain.exception.EmployeeNotAssignedException;
import com.pragma.powerup.domain.exception.InactiveDishException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.exception.RepeatedDishException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.OrderDishModel;
import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.domain.spi.IOrderPersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantEmployeePersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseTest {
    private static final Long CLIENT_ID = 20L;
    private static final Long RESTAURANT_ID = 1L;
    private static final Long OTHER_RESTAURANT_ID = 2L;
    private static final Long BURGER_ID = 4L;
    private static final Long PIZZA_ID = 5L;
    private static final Long EMPLOYEE_ID = 24L;
    private static final List<OrderStatus> IN_PROGRESS_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION, OrderStatus.READY);

    @Mock
    private IOrderPersistencePort orderPersistencePort;

    @Mock
    private IRestaurantPersistencePort restaurantPersistencePort;

    @Mock
    private IDishPersistencePort dishPersistencePort;

    @Mock
    private IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort;

    @InjectMocks
    private OrderUseCase orderUseCase;

    private RestaurantModel buildRestaurant(Long id) {
        RestaurantModel restaurant = new RestaurantModel();
        restaurant.setId(id);
        return restaurant;
    }

    private DishModel buildDish(Long id, Long restaurantId, boolean active) {
        DishModel dish = new DishModel();
        dish.setId(id);
        dish.setName("Dish " + id);
        dish.setActive(active);
        dish.setRestaurant(buildRestaurant(restaurantId));
        return dish;
    }

    private OrderDishModel buildItem(Long dishId, int quantity) {
        DishModel dish = new DishModel();
        dish.setId(dishId);
        OrderDishModel item = new OrderDishModel();
        item.setDish(dish);
        item.setQuantity(quantity);
        return item;
    }

    private OrderModel buildOrder(OrderDishModel... items) {
        OrderModel order = new OrderModel();
        order.setRestaurant(buildRestaurant(RESTAURANT_ID));
        order.setDishes(List.of(items));
        return order;
    }

    private void givenClientWithoutOrderInProgressAndExistingRestaurant() {
        when(orderPersistencePort.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES)).thenReturn(false);
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID))
                .thenReturn(Optional.of(buildRestaurant(RESTAURANT_ID)));
    }

    @Test
    void createOrderWithValidDataSavesItAsPendingForTheClientOfTheToken() {
        OrderModel order = buildOrder(buildItem(BURGER_ID, 2), buildItem(PIZZA_ID, 1));
        DishModel burger = buildDish(BURGER_ID, RESTAURANT_ID, true);
        DishModel pizza = buildDish(PIZZA_ID, RESTAURANT_ID, true);
        OrderModel savedOrder = new OrderModel();
        givenClientWithoutOrderInProgressAndExistingRestaurant();
        when(dishPersistencePort.findDishById(BURGER_ID)).thenReturn(Optional.of(burger));
        when(dishPersistencePort.findDishById(PIZZA_ID)).thenReturn(Optional.of(pizza));
        when(orderPersistencePort.saveOrder(any(OrderModel.class))).thenReturn(savedOrder);
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);

        OrderModel result = orderUseCase.createOrder(order, CLIENT_ID);

        assertSame(savedOrder, result);
        ArgumentCaptor<OrderModel> orderCaptor = ArgumentCaptor.forClass(OrderModel.class);
        verify(orderPersistencePort).saveOrder(orderCaptor.capture());
        OrderModel orderToSave = orderCaptor.getValue();
        assertAll(
                () -> assertEquals(CLIENT_ID, orderToSave.getClientId()),
                () -> assertEquals(OrderStatus.PENDING, orderToSave.getStatus()),
                () -> assertEquals(RESTAURANT_ID, orderToSave.getRestaurant().getId()),
                () -> assertNotNull(orderToSave.getDate()),
                () -> assertTrue(orderToSave.getDate().isAfter(before)),
                () -> assertNull(orderToSave.getEmployeeId()),
                () -> assertSame(burger, orderToSave.getDishes().get(0).getDish()),
                () -> assertEquals(2, orderToSave.getDishes().get(0).getQuantity()),
                () -> assertSame(pizza, orderToSave.getDishes().get(1).getDish()),
                () -> assertEquals(1, orderToSave.getDishes().get(1).getQuantity())
        );
    }

    @Test
    void createOrderWhenTheClientHasAnOrderInProgressThrowsOrderInProgressExceptionWithoutCheckingAnythingElse() {
        OrderModel order = buildOrder(buildItem(BURGER_ID, 1));
        when(orderPersistencePort.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES)).thenReturn(true);

        OrderInProgressException exception = assertThrows(OrderInProgressException.class,
                () -> orderUseCase.createOrder(order, CLIENT_ID));

        assertEquals("You already have an order in progress", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
        verifyNoInteractions(restaurantPersistencePort, dishPersistencePort);
    }

    @Test
    void createOrderInAMissingRestaurantThrowsRestaurantNotFoundException() {
        OrderModel order = buildOrder(buildItem(BURGER_ID, 1));
        when(orderPersistencePort.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES)).thenReturn(false);
        when(restaurantPersistencePort.findRestaurantById(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> orderUseCase.createOrder(order, CLIENT_ID));

        verify(orderPersistencePort, never()).saveOrder(any());
        verifyNoInteractions(dishPersistencePort);
    }

    @Test
    void createOrderWithTheSameDishTwiceThrowsRepeatedDishException() {
        OrderModel order = buildOrder(buildItem(BURGER_ID, 1), buildItem(BURGER_ID, 2));
        givenClientWithoutOrderInProgressAndExistingRestaurant();

        RepeatedDishException exception = assertThrows(RepeatedDishException.class,
                () -> orderUseCase.createOrder(order, CLIENT_ID));

        assertEquals("Each dish can appear only once in the order; use the quantity instead",
                exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
        verifyNoInteractions(dishPersistencePort);
    }

    @Test
    void createOrderWithAMissingDishThrowsDishNotFoundException() {
        OrderModel order = buildOrder(buildItem(99L, 1));
        givenClientWithoutOrderInProgressAndExistingRestaurant();
        when(dishPersistencePort.findDishById(99L)).thenReturn(Optional.empty());

        DishNotFoundException exception = assertThrows(DishNotFoundException.class,
                () -> orderUseCase.createOrder(order, CLIENT_ID));

        assertEquals("The requested dish does not exist", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrderWithADishOfAnotherRestaurantThrowsDishNotFromRestaurantException() {
        OrderModel order = buildOrder(buildItem(BURGER_ID, 1));
        givenClientWithoutOrderInProgressAndExistingRestaurant();
        when(dishPersistencePort.findDishById(BURGER_ID))
                .thenReturn(Optional.of(buildDish(BURGER_ID, OTHER_RESTAURANT_ID, true)));

        DishNotFromRestaurantException exception = assertThrows(DishNotFromRestaurantException.class,
                () -> orderUseCase.createOrder(order, CLIENT_ID));

        assertEquals("The dish 4 does not belong to the selected restaurant", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void createOrderWithADisabledDishThrowsInactiveDishException() {
        OrderModel order = buildOrder(buildItem(BURGER_ID, 1));
        givenClientWithoutOrderInProgressAndExistingRestaurant();
        when(dishPersistencePort.findDishById(BURGER_ID))
                .thenReturn(Optional.of(buildDish(BURGER_ID, RESTAURANT_ID, false)));

        InactiveDishException exception = assertThrows(InactiveDishException.class,
                () -> orderUseCase.createOrder(order, CLIENT_ID));

        assertEquals("The dish 4 is not available", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void listOrdersByStatusReturnsThePageOfOrdersOfTheRestaurantOfTheEmployee() {
        PageModel<OrderModel> expectedPage = new PageModel<>(List.of(new OrderModel()), 0, 10, 1L, 1);
        when(restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(EMPLOYEE_ID))
                .thenReturn(Optional.of(RESTAURANT_ID));
        when(orderPersistencePort.findOrdersByRestaurantAndStatus(RESTAURANT_ID, OrderStatus.PENDING, 0, 10))
                .thenReturn(expectedPage);

        PageModel<OrderModel> result = orderUseCase.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, 0, 10);

        assertSame(expectedPage, result);
        verify(orderPersistencePort).findOrdersByRestaurantAndStatus(RESTAURANT_ID, OrderStatus.PENDING, 0, 10);
    }

    @Test
    void listOrdersByStatusWhenTheEmployeeIsNotAssignedThrowsEmployeeNotAssignedException() {
        when(restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        EmployeeNotAssignedException exception = assertThrows(EmployeeNotAssignedException.class,
                () -> orderUseCase.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, 0, 10));

        assertEquals("The employee is not assigned to any restaurant", exception.getMessage());
        verify(orderPersistencePort, never()).findOrdersByRestaurantAndStatus(any(), any(), anyInt(), anyInt());
    }

    @Test
    void listOrdersByStatusWithInvalidPaginationThrowsBeforeTouchingAnyPort() {
        assertThrows(InvalidPaginationException.class,
                () -> orderUseCase.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, -1, 10));

        verifyNoInteractions(restaurantEmployeePersistencePort, orderPersistencePort);
    }
}
