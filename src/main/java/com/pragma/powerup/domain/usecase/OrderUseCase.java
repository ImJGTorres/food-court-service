package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IOrderServicePort;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.DishNotFromRestaurantException;
import com.pragma.powerup.domain.exception.EmployeeNotAssignedException;
import com.pragma.powerup.domain.exception.InactiveDishException;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.exception.OrderNotAssignedToEmployeeException;
import com.pragma.powerup.domain.exception.OrderNotFoundException;
import com.pragma.powerup.domain.exception.OrderNotFromEmployeeRestaurantException;
import com.pragma.powerup.domain.exception.OrderNotInPreparationException;
import com.pragma.powerup.domain.exception.OrderNotPendingException;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class OrderUseCase implements IOrderServicePort {
    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");
    private static final List<OrderStatus> IN_PROGRESS_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION, OrderStatus.READY);
    private static final String ORDER_IN_PROGRESS_MESSAGE = "You already have an order in progress";
    private static final String RESTAURANT_NOT_FOUND_MESSAGE = "The requested restaurant does not exist";
    private static final String REPEATED_DISH_MESSAGE =
            "Each dish can appear only once in the order; use the quantity instead";
    private static final String DISH_NOT_FOUND_MESSAGE = "The requested dish does not exist";
    private static final String DISH_NOT_FROM_RESTAURANT_MESSAGE = "The dish %d does not belong to the selected restaurant";
    private static final String INACTIVE_DISH_MESSAGE = "The dish %d is not available";
    private static final String EMPLOYEE_NOT_ASSIGNED_MESSAGE = "The employee is not assigned to any restaurant";
    private static final String ORDER_NOT_FOUND_MESSAGE = "The requested order does not exist";
    private static final String ORDER_NOT_FROM_EMPLOYEE_RESTAURANT_MESSAGE =
            "The order does not belong to the restaurant of the employee";
    private static final String ORDER_NOT_PENDING_MESSAGE = "Only pending orders can be assigned";
    private static final String ORDER_NOT_IN_PREPARATION_MESSAGE =
            "Only orders in preparation can be marked as ready";
    private static final String ORDER_NOT_ASSIGNED_TO_EMPLOYEE_MESSAGE =
            "Only the employee assigned to the order can mark it as ready";
    private static final String ORDER_READY_SMS = "Pedido #%d listo en %s. Tu PIN de seguridad es %s.";

    private final IOrderPersistencePort orderPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IDishPersistencePort dishPersistencePort;
    private final IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort;
    private final IPinGeneratorPort pinGeneratorPort;
    private final INotificationPort notificationPort;
    private final IUserClientPort userClientPort;

    public OrderUseCase(IOrderPersistencePort orderPersistencePort,
                        IRestaurantPersistencePort restaurantPersistencePort,
                        IDishPersistencePort dishPersistencePort,
                        IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort,
                        IPinGeneratorPort pinGeneratorPort,
                        INotificationPort notificationPort,
                        IUserClientPort userClientPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.dishPersistencePort = dishPersistencePort;
        this.restaurantEmployeePersistencePort = restaurantEmployeePersistencePort;
        this.pinGeneratorPort = pinGeneratorPort;
        this.notificationPort = notificationPort;
        this.userClientPort = userClientPort;
    }

    @Override
    public OrderModel createOrder(OrderModel orderModel, Long clientId) {
        validateNoOrderInProgress(clientId);
        RestaurantModel restaurant = findRestaurant(orderModel.getRestaurant().getId());
        validateNoRepeatedDishes(orderModel.getDishes());
        for (OrderDishModel item : orderModel.getDishes()) {
            item.setDish(findOrderableDish(item.getDish().getId(), restaurant.getId()));
        }
        orderModel.setRestaurant(restaurant);
        orderModel.setClientId(clientId);
        orderModel.setDate(LocalDateTime.now(COLOMBIA_ZONE));
        orderModel.setStatus(OrderStatus.PENDING);
        return orderPersistencePort.saveOrder(orderModel);
    }

    @Override
    public PageModel<OrderModel> listOrdersByStatus(Long employeeId, OrderStatus status, int page, int size) {
        PaginationValidator.validate(page, size);
        Long restaurantId = findRestaurantIdOfEmployee(employeeId);
        return orderPersistencePort.findOrdersByRestaurantAndStatus(restaurantId, status, page, size);
    }

    @Override
    public OrderModel assignOrder(Long orderId, Long employeeId) {
        OrderModel order = findOrder(orderId);
        if (!order.getRestaurant().getId().equals(findRestaurantIdOfEmployee(employeeId))) {
            throw new OrderNotFromEmployeeRestaurantException(ORDER_NOT_FROM_EMPLOYEE_RESTAURANT_MESSAGE);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPendingException(ORDER_NOT_PENDING_MESSAGE);
        }
        order.setEmployeeId(employeeId);
        order.setStatus(OrderStatus.IN_PREPARATION);
        return orderPersistencePort.saveOrder(order);
    }

    @Override
    public OrderModel markOrderReady(Long orderId, Long employeeId) {
        OrderModel order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.IN_PREPARATION) {
            throw new OrderNotInPreparationException(ORDER_NOT_IN_PREPARATION_MESSAGE);
        }
        if (!employeeId.equals(order.getEmployeeId())) {
            throw new OrderNotAssignedToEmployeeException(ORDER_NOT_ASSIGNED_TO_EMPLOYEE_MESSAGE);
        }
        order.setSecurityPin(pinGeneratorPort.generatePin());
        order.setStatus(OrderStatus.READY);
        OrderModel savedOrder = orderPersistencePort.saveOrder(order);
        notificationPort.sendSms(userClientPort.getClientPhone(order.getClientId()), buildReadySms(order));
        return savedOrder;
    }

    private OrderModel findOrder(Long orderId) {
        return orderPersistencePort.findOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE));
    }

    private String buildReadySms(OrderModel order) {
        return String.format(ORDER_READY_SMS, order.getId(), order.getRestaurant().getName(), order.getSecurityPin());
    }

    private Long findRestaurantIdOfEmployee(Long employeeId) {
        return restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(employeeId)
                .orElseThrow(() -> new EmployeeNotAssignedException(EMPLOYEE_NOT_ASSIGNED_MESSAGE));
    }

    private void validateNoOrderInProgress(Long clientId) {
        if (orderPersistencePort.existsByClientIdAndStatusIn(clientId, IN_PROGRESS_STATUSES)) {
            throw new OrderInProgressException(ORDER_IN_PROGRESS_MESSAGE);
        }
    }

    private RestaurantModel findRestaurant(Long restaurantId) {
        return restaurantPersistencePort.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(RESTAURANT_NOT_FOUND_MESSAGE));
    }

    private void validateNoRepeatedDishes(List<OrderDishModel> items) {
        long distinctDishes = items.stream().map(item -> item.getDish().getId()).distinct().count();
        if (distinctDishes != items.size()) {
            throw new RepeatedDishException(REPEATED_DISH_MESSAGE);
        }
    }

    private DishModel findOrderableDish(Long dishId, Long restaurantId) {
        DishModel dish = dishPersistencePort.findDishById(dishId)
                .orElseThrow(() -> new DishNotFoundException(DISH_NOT_FOUND_MESSAGE));
        if (!dish.getRestaurant().getId().equals(restaurantId)) {
            throw new DishNotFromRestaurantException(String.format(DISH_NOT_FROM_RESTAURANT_MESSAGE, dishId));
        }
        if (!Boolean.TRUE.equals(dish.getActive())) {
            throw new InactiveDishException(String.format(INACTIVE_DISH_MESSAGE, dishId));
        }
        return dish;
    }
}
