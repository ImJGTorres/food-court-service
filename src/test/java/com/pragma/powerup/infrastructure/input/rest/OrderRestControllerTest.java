package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderDishResponseDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.handler.IOrderHandler;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.DishNotFromRestaurantException;
import com.pragma.powerup.domain.exception.InactiveDishException;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.exception.RepeatedDishException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.infrastructure.exceptionhandler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderRestControllerTest {
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final Long TOKEN_CLIENT_ID = 20L;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        clientAuthentication = new UsernamePasswordAuthenticationToken(TOKEN_CLIENT_ID, null, List.of());
    }

    @Test
    void createOrderWithValidRequestReturns201AndThePendingOrder() throws Exception {
        OrderDishResponseDto dish = new OrderDishResponseDto();
        dish.setDishId(4L);
        dish.setName("Hamburguesa");
        dish.setQuantity(2);
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1L);
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
                .andExpect(jsonPath("$.clientId").doesNotExist());
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
}
