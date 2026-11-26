package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderDishResponseDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.handler.IOrderHandler;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.DishNotFromRestaurantException;
import com.pragma.powerup.domain.exception.EmployeeNotAssignedException;
import com.pragma.powerup.domain.exception.InactiveDishException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.MessagingServiceUnavailableException;
import com.pragma.powerup.domain.exception.NotificationRejectedException;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.exception.OrderNotAssignedToEmployeeException;
import com.pragma.powerup.domain.exception.OrderNotFoundException;
import com.pragma.powerup.domain.exception.OrderNotFromEmployeeRestaurantException;
import com.pragma.powerup.domain.exception.OrderNotInPreparationException;
import com.pragma.powerup.domain.exception.OrderNotPendingException;
import com.pragma.powerup.domain.exception.RepeatedDishException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.infrastructure.exceptionhandler.GlobalExceptionHandler;
import com.pragma.powerup.infrastructure.out.jpa.entity.OrderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderRestControllerTest {
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String ASSIGN_ORDER_URL = "/api/v1/orders/1/assign";
    private static final String READY_ORDER_URL = "/api/v1/orders/1/ready";
    private static final Long TOKEN_CLIENT_ID = 20L;
    private static final Long TOKEN_EMPLOYEE_ID = 24L;
    private static final String VALID_ORDER_JSON = """
            {
              "restaurantId": 1,
              "dishes": [
                {"dishId": 4, "quantity": 2},
                {"dishId": 5, "quantity": 1}
              ]
            }
            """;

    @Mock
    private IOrderHandler orderHandler;

    @InjectMocks
    private OrderRestController orderRestController;

    private MockMvc mockMvc;
    private Authentication clientAuthentication;
    private Authentication employeeAuthentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        clientAuthentication = new UsernamePasswordAuthenticationToken(TOKEN_CLIENT_ID, null, List.of());
        employeeAuthentication = new UsernamePasswordAuthenticationToken(TOKEN_EMPLOYEE_ID, null, List.of());
    }

    @Test
    void createOrderWithValidRequestReturns201AndThePendingOrder() throws Exception {
        OrderDishResponseDto dish = new OrderDishResponseDto();
        dish.setDishId(4L);
        dish.setName("Hamburguesa");
        dish.setQuantity(2);
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1L);
        response.setClientId(TOKEN_CLIENT_ID);
        response.setRestaurantId(1L);
        response.setDate(LocalDateTime.of(2026, 11, 22, 10, 15, 30));
        response.setStatus("PENDING");
        response.setDishes(List.of(dish));
        when(orderHandler.createOrder(any(OrderRequestDto.class), eq(TOKEN_CLIENT_ID))).thenReturn(response);

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.restaurantId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.dishes[0].dishId").value(4))
                .andExpect(jsonPath("$.dishes[0].name").value("Hamburguesa"))
                .andExpect(jsonPath("$.dishes[0].quantity").value(2))
                .andExpect(jsonPath("$.clientId").value(20))
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    @Test
    void createOrderWithoutRestaurantAndWithoutDishesReturns400ForBothFields() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.restaurantId").exists())
                .andExpect(jsonPath("$.dishes").exists());

        verifyNoInteractions(orderHandler);
    }

    @Test
    void createOrderWithAnEmptyListOfDishesReturns400() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\": 1, \"dishes\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dishes").exists());

        verifyNoInteractions(orderHandler);
    }

    @Test
    void createOrderWithAQuantityOfZeroAndAMissingDishIdReturns400ForTheItemsInsideTheList() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\": 1, \"dishes\": [{\"dishId\": 4, \"quantity\": 0}, {\"quantity\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['dishes[0].quantity']").exists())
                .andExpect(jsonPath("$['dishes[1].dishId']").exists());

        verifyNoInteractions(orderHandler);
    }

    @Test
    void createOrderWithBrokenJsonReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verifyNoInteractions(orderHandler);
    }

    @Test
    void createOrderWhenTheClientHasAnOrderInProgressReturns409() throws Exception {
        whenTheHandlerThrows(new OrderInProgressException("You already have an order in progress"));

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have an order in progress"));
    }

    @Test
    void createOrderWithARepeatedDishReturns400() throws Exception {
        whenTheHandlerThrows(new RepeatedDishException(
                "Each dish can appear only once in the order; use the quantity instead"));

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Each dish can appear only once in the order; use the quantity instead"));
    }

    @Test
    void createOrderWithADishOfAnotherRestaurantReturns400() throws Exception {
        whenTheHandlerThrows(new DishNotFromRestaurantException("The dish 6 does not belong to the selected restaurant"));

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The dish 6 does not belong to the selected restaurant"));
    }

    @Test
    void createOrderWithADisabledDishReturns400() throws Exception {
        whenTheHandlerThrows(new InactiveDishException("The dish 7 is not available"));

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The dish 7 is not available"));
    }

    @Test
    void createOrderInAMissingRestaurantReturns404() throws Exception {
        whenTheHandlerThrows(new RestaurantNotFoundException("The requested restaurant does not exist"));

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested restaurant does not exist"));
    }

    @Test
    void createOrderWithAMissingDishReturns404() throws Exception {
        whenTheHandlerThrows(new DishNotFoundException("The requested dish does not exist"));

        mockMvc.perform(post(ORDERS_URL)
                        .principal(clientAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested dish does not exist"));
    }

    private void whenTheHandlerThrows(RuntimeException exception) {
        when(orderHandler.createOrder(any(OrderRequestDto.class), eq(TOKEN_CLIENT_ID))).thenThrow(exception);
    }

    @Test
    void listOrdersByStatusReturns200WithAllTheFieldsOfEachOrder() throws Exception {
        OrderDishResponseDto dish = new OrderDishResponseDto();
        dish.setDishId(4L);
        dish.setName("Hamburguesa");
        dish.setQuantity(2);
        OrderResponseDto order = new OrderResponseDto();
        order.setId(1L);
        order.setClientId(21L);
        order.setRestaurantId(1L);
        order.setDate(LocalDateTime.of(2026, 11, 22, 10, 38, 42));
        order.setStatus("PENDING");
        order.setDishes(List.of(dish));
        when(orderHandler.listOrdersByStatus(TOKEN_EMPLOYEE_ID, OrderStatus.PENDING, 0, 10))
                .thenReturn(new PageResponseDto<>(List.of(order), 0, 10, 1L, 1));

        mockMvc.perform(get(ORDERS_URL).principal(employeeAuthentication).param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].clientId").value(21))
                .andExpect(jsonPath("$.content[0].restaurantId").value(1))
                .andExpect(jsonPath("$.content[0].date").exists())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].dishes[0].name").value("Hamburguesa"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listOrdersByStatusPassesTheEmployeeOfTheTokenTheStatusAndThePageToTheHandler() throws Exception {
        mockMvc.perform(get(ORDERS_URL).principal(employeeAuthentication)
                        .param("status", "READY").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(orderHandler).listOrdersByStatus(TOKEN_EMPLOYEE_ID, OrderStatus.READY, 2, 5);
    }

    @Test
    void listOrdersWithoutStatusReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(get(ORDERS_URL).principal(employeeAuthentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'status'"));

        verifyNoInteractions(orderHandler);
    }

    @Test
    void listOrdersWithAStatusThatDoesNotExistReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(get(ORDERS_URL).principal(employeeAuthentication).param("status", "LISTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));

        verifyNoInteractions(orderHandler);
    }

    @Test
    void listOrdersWhenTheEmployeeIsNotAssignedReturns403() throws Exception {
        when(orderHandler.listOrdersByStatus(TOKEN_EMPLOYEE_ID, OrderStatus.PENDING, 0, 10))
                .thenThrow(new EmployeeNotAssignedException("The employee is not assigned to any restaurant"));

        mockMvc.perform(get(ORDERS_URL).principal(employeeAuthentication).param("status", "PENDING"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("The employee is not assigned to any restaurant"));
    }

    @Test
    void listOrdersWithInvalidPaginationReturns400() throws Exception {
        when(orderHandler.listOrdersByStatus(TOKEN_EMPLOYEE_ID, OrderStatus.PENDING, 0, 51))
                .thenThrow(new InvalidPaginationException("The page size must be between 1 and 50"));

        mockMvc.perform(get(ORDERS_URL).principal(employeeAuthentication)
                        .param("status", "PENDING").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The page size must be between 1 and 50"));
    }

    @Test
    void assignOrderReturns200WithTheOrderInPreparationAndTheEmployeeOfTheToken() throws Exception {
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1L);
        response.setStatus("IN_PREPARATION");
        response.setEmployeeId(TOKEN_EMPLOYEE_ID);
        when(orderHandler.assignOrder(1L, TOKEN_EMPLOYEE_ID)).thenReturn(response);

        mockMvc.perform(patch(ASSIGN_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PREPARATION"))
                .andExpect(jsonPath("$.employeeId").value(24));
    }

    @Test
    void assignAMissingOrderReturns404() throws Exception {
        when(orderHandler.assignOrder(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new OrderNotFoundException("The requested order does not exist"));

        mockMvc.perform(patch(ASSIGN_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested order does not exist"));
    }

    @Test
    void assignAnOrderOfAnotherRestaurantReturns403() throws Exception {
        when(orderHandler.assignOrder(1L, TOKEN_EMPLOYEE_ID)).thenThrow(
                new OrderNotFromEmployeeRestaurantException("The order does not belong to the restaurant of the employee"));

        mockMvc.perform(patch(ASSIGN_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("The order does not belong to the restaurant of the employee"));
    }

    @Test
    void assignOrderWhenTheEmployeeIsNotAssignedToARestaurantReturns403() throws Exception {
        when(orderHandler.assignOrder(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new EmployeeNotAssignedException("The employee is not assigned to any restaurant"));

        mockMvc.perform(patch(ASSIGN_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignAnOrderThatIsNotPendingReturns409() throws Exception {
        when(orderHandler.assignOrder(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new OrderNotPendingException("Only pending orders can be assigned"));

        mockMvc.perform(patch(ASSIGN_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only pending orders can be assigned"));
    }

    @Test
    void assignAnOrderThatSomeoneElseChangedAtTheSameTimeReturns409() throws Exception {
        when(orderHandler.assignOrder(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new ObjectOptimisticLockingFailureException(OrderEntity.class, 1L));

        mockMvc.perform(patch(ASSIGN_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The order was modified by someone else, try again"));
    }

    @Test
    void assignAnOrderWithANonNumericIdReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(patch(ORDERS_URL + "/abc/assign").principal(employeeAuthentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'orderId'"));

        verifyNoInteractions(orderHandler);
    }

    @Test
    void markOrderReadyReturns200WithTheReadyOrderAndNeverShowsThePin() throws Exception {
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1L);
        response.setStatus("READY");
        response.setEmployeeId(TOKEN_EMPLOYEE_ID);
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID)).thenReturn(response);

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.employeeId").value(24))
                .andExpect(jsonPath("$.securityPin").doesNotExist())
                .andExpect(jsonPath("$.pin").doesNotExist());
    }

    @Test
    void markAMissingOrderAsReadyReturns404() throws Exception {
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new OrderNotFoundException("The requested order does not exist"));

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAnOrderThatIsNotInPreparationAsReadyReturns409() throws Exception {
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new OrderNotInPreparationException("Only orders in preparation can be marked as ready"));

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only orders in preparation can be marked as ready"));
    }

    @Test
    void markAnOrderAssignedToAnotherEmployeeAsReadyReturns403() throws Exception {
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID)).thenThrow(
                new OrderNotAssignedToEmployeeException("Only the employee assigned to the order can mark it as ready"));

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the employee assigned to the order can mark it as ready"));
    }

    @Test
    void markOrderReadyWhenTheClientPhoneCannotReceiveSmsReturns422() throws Exception {
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new NotificationRejectedException("The client phone number cannot receive SMS"));

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("The client phone number cannot receive SMS"));
    }

    @Test
    void markOrderReadyWhenTheMessagingServiceIsDownReturns503() throws Exception {
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new MessagingServiceUnavailableException("The messaging service is not available"));

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The messaging service is not available"));
    }

    @Test
    void markOrderReadyWhenTheUsersServiceIsDownReturns503() throws Exception {
        when(orderHandler.markOrderReady(1L, TOKEN_EMPLOYEE_ID))
                .thenThrow(new UserServiceUnavailableException("The users service is not available"));

        mockMvc.perform(patch(READY_ORDER_URL).principal(employeeAuthentication))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void markOrderReadyWithANonNumericIdReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(patch(ORDERS_URL + "/abc/ready").principal(employeeAuthentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'orderId'"));

        verifyNoInteractions(orderHandler);
    }
}
