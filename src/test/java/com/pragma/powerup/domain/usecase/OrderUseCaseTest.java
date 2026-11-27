package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.DishNotFromRestaurantException;
import com.pragma.powerup.domain.exception.EmployeeNotAssignedException;
import com.pragma.powerup.domain.exception.InactiveDishException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.InvalidSecurityPinException;
import com.pragma.powerup.domain.exception.MessagingServiceUnavailableException;
import com.pragma.powerup.domain.exception.NotificationRejectedException;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.exception.OrderLockedException;
import com.pragma.powerup.domain.exception.OrderNotAssignedToEmployeeException;
import com.pragma.powerup.domain.exception.OrderNotFoundException;
import com.pragma.powerup.domain.exception.OrderNotFromEmployeeRestaurantException;
import com.pragma.powerup.domain.exception.OrderNotInPreparationException;
import com.pragma.powerup.domain.exception.OrderNotPendingException;
import com.pragma.powerup.domain.exception.OrderNotReadyException;
import com.pragma.powerup.domain.exception.RepeatedDishException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.model.DishModel;
import com.pragma.powerup.domain.model.OrderDishModel;
import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.pragma.powerup.domain.spi.IDishPersistencePort;
import com.pragma.powerup.domain.spi.INotificationPort;
import com.pragma.powerup.domain.spi.IOrderPersistencePort;
import com.pragma.powerup.domain.spi.IPinGeneratorPort;
import com.pragma.powerup.domain.spi.IRestaurantEmployeePersistencePort;
import com.pragma.powerup.domain.spi.IRestaurantPersistencePort;
import com.pragma.powerup.domain.spi.IUserClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
    private static final Long ORDER_ID = 1L;
    private static final String PIN = "482913";
    private static final String CLIENT_PHONE = "+573005698325";
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

    @Mock
    private IPinGeneratorPort pinGeneratorPort;

    @Mock
    private INotificationPort notificationPort;

    @Mock
    private IUserClientPort userClientPort;

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

    private OrderModel buildExistingOrder(Long restaurantId, OrderStatus status) {
        OrderModel order = new OrderModel();
        order.setId(ORDER_ID);
        order.setRestaurant(buildRestaurant(restaurantId));
        order.setStatus(status);
        return order;
    }

    @Test
    void assignOrderSetsTheEmployeeOfTheTokenAndMovesThePendingOrderToInPreparation() {
        OrderModel order = buildExistingOrder(RESTAURANT_ID, OrderStatus.PENDING);
        OrderModel savedOrder = new OrderModel();
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));
        when(restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(EMPLOYEE_ID))
                .thenReturn(Optional.of(RESTAURANT_ID));
        when(orderPersistencePort.saveOrder(any(OrderModel.class))).thenReturn(savedOrder);

        OrderModel result = orderUseCase.assignOrder(ORDER_ID, EMPLOYEE_ID);

        assertSame(savedOrder, result);
        ArgumentCaptor<OrderModel> orderCaptor = ArgumentCaptor.forClass(OrderModel.class);
        verify(orderPersistencePort).saveOrder(orderCaptor.capture());
        assertAll(
                () -> assertEquals(ORDER_ID, orderCaptor.getValue().getId()),
                () -> assertEquals(EMPLOYEE_ID, orderCaptor.getValue().getEmployeeId()),
                () -> assertEquals(OrderStatus.IN_PREPARATION, orderCaptor.getValue().getStatus())
        );
    }

    @Test
    void assignAMissingOrderThrowsOrderNotFoundExceptionWithoutLookingForTheEmployee() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderUseCase.assignOrder(ORDER_ID, EMPLOYEE_ID));

        assertEquals("The requested order does not exist", exception.getMessage());
        verifyNoInteractions(restaurantEmployeePersistencePort);
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void assignOrderWhenTheEmployeeIsNotAssignedToARestaurantThrowsEmployeeNotAssignedException() {
        when(orderPersistencePort.findOrderById(ORDER_ID))
                .thenReturn(Optional.of(buildExistingOrder(RESTAURANT_ID, OrderStatus.PENDING)));
        when(restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotAssignedException.class, () -> orderUseCase.assignOrder(ORDER_ID, EMPLOYEE_ID));

        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void assignAnOrderOfAnotherRestaurantThrowsOrderNotFromEmployeeRestaurantException() {
        when(orderPersistencePort.findOrderById(ORDER_ID))
                .thenReturn(Optional.of(buildExistingOrder(OTHER_RESTAURANT_ID, OrderStatus.PENDING)));
        when(restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(EMPLOYEE_ID))
                .thenReturn(Optional.of(RESTAURANT_ID));

        OrderNotFromEmployeeRestaurantException exception = assertThrows(
                OrderNotFromEmployeeRestaurantException.class, () -> orderUseCase.assignOrder(ORDER_ID, EMPLOYEE_ID));

        assertEquals("The order does not belong to the restaurant of the employee", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @Test
    void assignAnOrderAlreadyInPreparationThrowsOrderNotPendingException() {
        assertTheOrderCannotBeAssignedWhenItIs(OrderStatus.IN_PREPARATION);
    }

    @Test
    void assignAReadyOrderThrowsOrderNotPendingException() {
        assertTheOrderCannotBeAssignedWhenItIs(OrderStatus.READY);
    }

    @Test
    void assignACancelledOrderThrowsOrderNotPendingException() {
        assertTheOrderCannotBeAssignedWhenItIs(OrderStatus.CANCELLED);
    }

    private void assertTheOrderCannotBeAssignedWhenItIs(OrderStatus status) {
        when(orderPersistencePort.findOrderById(ORDER_ID))
                .thenReturn(Optional.of(buildExistingOrder(RESTAURANT_ID, status)));
        when(restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(EMPLOYEE_ID))
                .thenReturn(Optional.of(RESTAURANT_ID));

        OrderNotPendingException exception = assertThrows(OrderNotPendingException.class,
                () -> orderUseCase.assignOrder(ORDER_ID, EMPLOYEE_ID));

        assertEquals("Only pending orders can be assigned", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    private OrderModel buildOrderInPreparation() {
        RestaurantModel restaurant = buildRestaurant(RESTAURANT_ID);
        restaurant.setName("Arepas");
        OrderModel order = buildExistingOrder(RESTAURANT_ID, OrderStatus.IN_PREPARATION);
        order.setRestaurant(restaurant);
        order.setClientId(CLIENT_ID);
        order.setEmployeeId(EMPLOYEE_ID);
        return order;
    }

    @Test
    void markOrderReadyGeneratesThePinSavesTheOrderAsReadyAndThenSendsTheSmsToTheClient() {
        OrderModel order = buildOrderInPreparation();
        OrderModel savedOrder = new OrderModel();
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));
        when(pinGeneratorPort.generatePin()).thenReturn(PIN);
        when(orderPersistencePort.saveOrder(any(OrderModel.class))).thenReturn(savedOrder);
        when(userClientPort.getClientPhone(CLIENT_ID)).thenReturn(CLIENT_PHONE);

        OrderModel result = orderUseCase.markOrderReady(ORDER_ID, EMPLOYEE_ID);

        assertSame(savedOrder, result);
        ArgumentCaptor<OrderModel> orderCaptor = ArgumentCaptor.forClass(OrderModel.class);
        InOrder inOrder = inOrder(orderPersistencePort, userClientPort, notificationPort);
        inOrder.verify(orderPersistencePort).saveOrder(orderCaptor.capture());
        inOrder.verify(userClientPort).getClientPhone(CLIENT_ID);
        inOrder.verify(notificationPort)
                .sendSms(CLIENT_PHONE, "Pedido #1 listo en Arepas. Tu PIN de seguridad es 482913.");
        assertAll(
                () -> assertEquals(OrderStatus.READY, orderCaptor.getValue().getStatus()),
                () -> assertEquals(PIN, orderCaptor.getValue().getSecurityPin()),
                () -> assertEquals(EMPLOYEE_ID, orderCaptor.getValue().getEmployeeId())
        );
    }

    @Test
    void markOrderReadyOfAMissingOrderThrowsOrderNotFoundExceptionWithoutTouchingAnythingElse() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderUseCase.markOrderReady(ORDER_ID, EMPLOYEE_ID));

        verifyNoInteractions(pinGeneratorPort, userClientPort, notificationPort);
        verify(orderPersistencePort, never()).saveOrder(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "IN_PREPARATION", mode = EnumSource.Mode.EXCLUDE)
    void markOrderReadyOfAnOrderThatIsNotInPreparationThrowsOrderNotInPreparationException(OrderStatus status) {
        OrderModel order = buildOrderInPreparation();
        order.setStatus(status);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));

        OrderNotInPreparationException exception = assertThrows(OrderNotInPreparationException.class,
                () -> orderUseCase.markOrderReady(ORDER_ID, EMPLOYEE_ID));

        assertEquals("Only orders in preparation can be marked as ready", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
        verifyNoInteractions(pinGeneratorPort, userClientPort, notificationPort);
    }

    @Test
    void markOrderReadyByAnEmployeeThatIsNotTheAssignedOneThrowsOrderNotAssignedToEmployeeException() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(buildOrderInPreparation()));

        OrderNotAssignedToEmployeeException exception = assertThrows(OrderNotAssignedToEmployeeException.class,
                () -> orderUseCase.markOrderReady(ORDER_ID, 99L));

        assertEquals("Only the employee assigned to the order can mark it as ready", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any());
        verifyNoInteractions(pinGeneratorPort, userClientPort, notificationPort);
    }

    @Test
    void markOrderReadyWhenTheClientPhoneCannotBeObtainedLetsTheExceptionPassAndSendsNoSms() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(buildOrderInPreparation()));
        when(pinGeneratorPort.generatePin()).thenReturn(PIN);
        when(orderPersistencePort.saveOrder(any(OrderModel.class))).thenReturn(new OrderModel());
        when(userClientPort.getClientPhone(CLIENT_ID))
                .thenThrow(new NotificationRejectedException("The client has no phone to notify"));

        assertThrows(NotificationRejectedException.class, () -> orderUseCase.markOrderReady(ORDER_ID, EMPLOYEE_ID));

        verify(notificationPort, never()).sendSms(anyString(), anyString());
    }

    @Test
    void markOrderReadyWhenTheMessagingServiceFailsLetsTheExceptionPassSoTheTransactionIsRolledBack() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(buildOrderInPreparation()));
        when(pinGeneratorPort.generatePin()).thenReturn(PIN);
        when(orderPersistencePort.saveOrder(any(OrderModel.class))).thenReturn(new OrderModel());
        when(userClientPort.getClientPhone(CLIENT_ID)).thenReturn(CLIENT_PHONE);
        doThrow(new MessagingServiceUnavailableException("The messaging service is not available"))
                .when(notificationPort).sendSms(anyString(), anyString());

        assertThrows(MessagingServiceUnavailableException.class,
                () -> orderUseCase.markOrderReady(ORDER_ID, EMPLOYEE_ID));
    }

    private static final String WRONG_PIN = "000000";

    private OrderModel buildReadyOrder(int failedPinAttempts) {
        OrderModel order = buildExistingOrder(RESTAURANT_ID, OrderStatus.READY);
        order.setClientId(CLIENT_ID);
        order.setEmployeeId(EMPLOYEE_ID);
        order.setSecurityPin(PIN);
        order.setFailedPinAttempts(failedPinAttempts);
        return order;
    }

    @Test
    void deliverOrderWithTheRightPinMarksItDeliveredErasesThePinAndSavesIt() {
        OrderModel order = buildReadyOrder(2);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderPersistencePort.saveOrder(order)).thenReturn(order);

        OrderModel result = orderUseCase.deliverOrder(ORDER_ID, EMPLOYEE_ID, PIN);

        assertAll(
                () -> assertSame(order, result),
                () -> assertEquals(OrderStatus.DELIVERED, result.getStatus()),
                () -> assertNull(result.getSecurityPin())
        );
        verify(orderPersistencePort).saveOrder(order);
    }

    @Test
    void deliverOrderWhenTheOrderDoesNotExistThrowsOrderNotFoundException() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderUseCase.deliverOrder(ORDER_ID, EMPLOYEE_ID, PIN));

        assertEquals("The requested order does not exist", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any(OrderModel.class));
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "READY", mode = EnumSource.Mode.EXCLUDE)
    void deliverOrderWhenTheOrderIsNotReadyThrowsOrderNotReadyException(OrderStatus status) {
        OrderModel order = buildReadyOrder(0);
        order.setStatus(status);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));

        OrderNotReadyException exception = assertThrows(OrderNotReadyException.class,
                () -> orderUseCase.deliverOrder(ORDER_ID, EMPLOYEE_ID, PIN));

        assertEquals("Only ready orders can be delivered", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any(OrderModel.class));
    }

    @Test
    void deliverOrderByAnEmployeeThatIsNotTheAssignedOneThrowsOrderNotAssignedToEmployeeException() {
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(buildReadyOrder(0)));

        OrderNotAssignedToEmployeeException exception = assertThrows(OrderNotAssignedToEmployeeException.class,
                () -> orderUseCase.deliverOrder(ORDER_ID, 99L, PIN));

        assertEquals("Only the employee assigned to the order can deliver it", exception.getMessage());
        verify(orderPersistencePort, never()).saveOrder(any(OrderModel.class));
    }

    @Test
    void deliverOrderWithAWrongPinCountsTheFailureSavesItAndTellsHowManyAttemptsAreLeft() {
        OrderModel order = buildReadyOrder(1);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));

        InvalidSecurityPinException exception = assertThrows(InvalidSecurityPinException.class,
                () -> orderUseCase.deliverOrder(ORDER_ID, EMPLOYEE_ID, WRONG_PIN));

        ArgumentCaptor<OrderModel> orderCaptor = ArgumentCaptor.forClass(OrderModel.class);
        verify(orderPersistencePort).saveOrder(orderCaptor.capture());
        assertAll(
                () -> assertEquals("Incorrect security PIN, 3 attempts left", exception.getMessage()),
                () -> assertEquals(2, orderCaptor.getValue().getFailedPinAttempts()),
                () -> assertEquals(OrderStatus.READY, orderCaptor.getValue().getStatus()),
                () -> assertEquals(PIN, orderCaptor.getValue().getSecurityPin())
        );
    }

    @Test
    void deliverOrderWithTheFifthWrongPinSavesTheFailureAndLocksTheOrder() {
        OrderModel order = buildReadyOrder(4);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));

        OrderLockedException exception = assertThrows(OrderLockedException.class,
                () -> orderUseCase.deliverOrder(ORDER_ID, EMPLOYEE_ID, WRONG_PIN));

        assertEquals("Too many incorrect PIN attempts, the order is locked", exception.getMessage());
        assertEquals(5, order.getFailedPinAttempts());
        verify(orderPersistencePort).saveOrder(order);
    }

    @Test
    void deliverOrderWhenTheOrderIsAlreadyLockedRefusesEvenTheRightPinAndSavesNothing() {
        OrderModel order = buildReadyOrder(5);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderLockedException.class, () -> orderUseCase.deliverOrder(ORDER_ID, EMPLOYEE_ID, PIN));

        assertAll(
                () -> assertEquals(OrderStatus.READY, order.getStatus()),
                () -> assertEquals(PIN, order.getSecurityPin())
        );
        verify(orderPersistencePort, never()).saveOrder(any(OrderModel.class));
    }

    @Test
    void deliverOrderChecksTheEmployeeBeforeThePinSoAStrangerCannotBurnTheAttempts() {
        OrderModel order = buildReadyOrder(0);
        when(orderPersistencePort.findOrderById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(OrderNotAssignedToEmployeeException.class,
                () -> orderUseCase.deliverOrder(ORDER_ID, 99L, WRONG_PIN));

        assertEquals(0, order.getFailedPinAttempts());
    }
}
