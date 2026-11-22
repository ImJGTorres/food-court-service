package com.pragma.powerup.domain.usecase;

import com.pragma.powerup.domain.api.IOrderServicePort;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.DishNotFromRestaurantException;
import com.pragma.powerup.domain.exception.EmployeeNotAssignedException;
import com.pragma.powerup.domain.exception.InactiveDishException;
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

    private final IOrderPersistencePort orderPersistencePort;
    private final IRestaurantPersistencePort restaurantPersistencePort;
    private final IDishPersistencePort dishPersistencePort;
    private final IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort;

    public OrderUseCase(IOrderPersistencePort orderPersistencePort,
                        IRestaurantPersistencePort restaurantPersistencePort,
                        IDishPersistencePort dishPersistencePort,
                        IRestaurantEmployeePersistencePort restaurantEmployeePersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
        this.restaurantPersistencePort = restaurantPersistencePort;
        this.dishPersistencePort = dishPersistencePort;
        this.restaurantEmployeePersistencePort = restaurantEmployeePersistencePort;
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
        Long restaurantId = restaurantEmployeePersistencePort.findRestaurantIdByEmployeeId(employeeId)
                .orElseThrow(() -> new EmployeeNotAssignedException(EMPLOYEE_NOT_ASSIGNED_MESSAGE));
        return orderPersistencePort.findOrdersByRestaurantAndStatus(restaurantId, status, page, size);
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
