package com.pragma.powerup.infrastructure.security;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.handler.IDishHandler;
import com.pragma.powerup.application.handler.IRestaurantHandler;
import com.pragma.powerup.infrastructure.input.rest.DishRestController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DishRestController.class, RestaurantRestController.class})
@Import({SecurityConfiguration.class, SecurityConfigurationTest.JwtTestConfiguration.class})
class SecurityConfigurationTest {
    private static final String RESTAURANTS_URL = "/api/v1/restaurants";
    private static final String DISHES_URL = "/api/v1/dishes";
    private static final String DISH_URL = "/api/v1/dishes/7";
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
}
