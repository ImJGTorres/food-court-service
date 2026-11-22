package com.pragma.powerup.infrastructure.security;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.handler.IDishHandler;
import com.pragma.powerup.application.handler.IOrderHandler;
import com.pragma.powerup.application.handler.IRestaurantEmployeeHandler;
import com.pragma.powerup.application.handler.IRestaurantHandler;
import com.pragma.powerup.infrastructure.input.rest.DishRestController;
import com.pragma.powerup.infrastructure.input.rest.OrderRestController;
import com.pragma.powerup.infrastructure.input.rest.RestaurantEmployeeRestController;
import com.pragma.powerup.infrastructure.input.rest.RestaurantRestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DishRestController.class, RestaurantRestController.class, OrderRestController.class,
        RestaurantEmployeeRestController.class})
@Import({SecurityConfiguration.class, SecurityConfigurationTest.JwtTestConfiguration.class})
class SecurityConfigurationTest {
    private static final String RESTAURANTS_URL = "/api/v1/restaurants";
    private static final String DISHES_URL = "/api/v1/dishes";
    private static final String DISH_URL = "/api/v1/dishes/7";
    private static final String DISH_STATUS_URL = "/api/v1/dishes/7/status";
    private static final String RESTAURANT_DISHES_URL = "/api/v1/restaurants/1/dishes";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String RESTAURANT_EMPLOYEES_URL = "/api/v1/restaurants/1/employees";
    private static final String AUTHORIZATION = "Authorization";

    @TestConfiguration
    static class JwtTestConfiguration {
        @Bean
        JwtTokenParser jwtTokenParser() {
            return new JwtTokenParser(JwtTestTokens.SECRET);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IDishHandler dishHandler;

    @MockitoBean
    private IRestaurantHandler restaurantHandler;

    @MockitoBean
    private IOrderHandler orderHandler;

    @MockitoBean
    private IRestaurantEmployeeHandler restaurantEmployeeHandler;

    private String bearerTokenFor(Long userId, String role) {
        return "Bearer " + JwtTestTokens.tokenFor(JwtTestTokens.SECRET, userId, role);
    }

    @Test
    void postRestaurantsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required: send a valid token"));
    }

    @Test
    void postRestaurantsWithOwnerTokenReturns403() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void postRestaurantsWithAdminTokenPassesSecurityAndReachesTheValidation() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postDishesWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postDishesWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void postDishesWithOwnerTokenPassesSecurityAndReachesTheValidation() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postDishesGivesTheHandlerTheUserIdOfTheTokenAsOwnerId() throws Exception {
        DishResponseDto response = new DishResponseDto();
        response.setId(1L);
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(8L))).thenReturn(response);

        mockMvc.perform(post(DISHES_URL)
                        .header(AUTHORIZATION, bearerTokenFor(8L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Hamburguesa",
                                  "price": 15000,
                                  "description": "Con queso",
                                  "imageUrl": "https://img.com/x.png",
                                  "categoryId": 1,
                                  "restaurantId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void patchDishWithoutTokenReturns401() throws Exception {
        mockMvc.perform(patch(DISH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchDishWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(patch(DISH_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchDishWithOwnerTokenPassesSecurityAndReachesTheValidation() throws Exception {
        mockMvc.perform(patch(DISH_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchDishStatusWithoutTokenReturns401() throws Exception {
        mockMvc.perform(patch(DISH_STATUS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchDishStatusWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(patch(DISH_STATUS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void patchDishStatusWithOwnerTokenPassesSecurityAndReachesTheValidation() throws Exception {
        mockMvc.perform(patch(DISH_STATUS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anyEndpointWithAnInvalidTokenReturns401() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .header(AUTHORIZATION, "Bearer not-a-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required: send a valid token"));
    }

    @Test
    void anyEndpointWithATokenSignedWithAnotherSecretReturns401() throws Exception {
        String foreignToken = JwtTestTokens.tokenFor(JwtTestTokens.OTHER_SECRET, 1L, "OWNER");

        mockMvc.perform(post(DISHES_URL)
                        .header(AUTHORIZATION, "Bearer " + foreignToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRestaurantsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRestaurantsWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL).header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRestaurantsWithOwnerTokenReturns403() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL).header(AUTHORIZATION, bearerTokenFor(1L, "OWNER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRestaurantsWithClientTokenReturns200() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL).header(AUTHORIZATION, bearerTokenFor(1L, "CLIENT")))
                .andExpect(status().isOk());
    }

    @Test
    void postRestaurantsWithClientTokenReturns403() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRestaurantDishesWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRestaurantDishesWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL).header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRestaurantDishesWithOwnerTokenReturns403() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL).header(AUTHORIZATION, bearerTokenFor(1L, "OWNER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRestaurantDishesWithEmployeeTokenReturns403() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL).header(AUTHORIZATION, bearerTokenFor(1L, "EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRestaurantDishesWithClientTokenReturns200() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL).header(AUTHORIZATION, bearerTokenFor(1L, "CLIENT")))
                .andExpect(status().isOk());
    }

    @Test
    void postOrdersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postOrdersWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postOrdersWithOwnerTokenReturns403() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postOrdersWithEmployeeTokenReturns403() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postOrdersWithClientTokenPassesSecurityAndReachesTheValidation() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postOrdersGivesTheHandlerTheUserIdOfTheTokenAsClientId() throws Exception {
        OrderResponseDto response = new OrderResponseDto();
        response.setId(1L);
        when(orderHandler.createOrder(any(OrderRequestDto.class), eq(20L))).thenReturn(response);

        mockMvc.perform(post(ORDERS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(20L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\": 1, \"dishes\": [{\"dishId\": 4, \"quantity\": 1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void postRestaurantEmployeesWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(RESTAURANT_EMPLOYEES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postRestaurantEmployeesWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(post(RESTAURANT_EMPLOYEES_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postRestaurantEmployeesWithClientTokenReturns403() throws Exception {
        mockMvc.perform(post(RESTAURANT_EMPLOYEES_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postRestaurantEmployeesWithOwnerTokenPassesSecurityAndReachesTheValidation() throws Exception {
        mockMvc.perform(post(RESTAURANT_EMPLOYEES_URL)
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrdersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ORDERS_URL).param("status", "PENDING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrdersWithClientTokenReturns403() throws Exception {
        mockMvc.perform(get(ORDERS_URL).param("status", "PENDING")
                        .header(AUTHORIZATION, bearerTokenFor(1L, "CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrdersWithOwnerTokenReturns403() throws Exception {
        mockMvc.perform(get(ORDERS_URL).param("status", "PENDING")
                        .header(AUTHORIZATION, bearerTokenFor(1L, "OWNER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrdersWithAdminTokenReturns403() throws Exception {
        mockMvc.perform(get(ORDERS_URL).param("status", "PENDING")
                        .header(AUTHORIZATION, bearerTokenFor(1L, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrdersWithEmployeeTokenReturns200() throws Exception {
        mockMvc.perform(get(ORDERS_URL).param("status", "PENDING")
                        .header(AUTHORIZATION, bearerTokenFor(24L, "EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void postOrdersWithEmployeeTokenIsStillForbiddenBecauseOnlyClientsPlaceOrders() throws Exception {
        mockMvc.perform(post(ORDERS_URL)
                        .header(AUTHORIZATION, bearerTokenFor(24L, "EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
