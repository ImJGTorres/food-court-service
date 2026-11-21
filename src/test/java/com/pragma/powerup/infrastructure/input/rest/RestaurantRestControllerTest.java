package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.RestaurantRequestDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.dto.response.RestaurantResponseDto;
import com.pragma.powerup.application.dto.response.RestaurantSummaryResponseDto;
import com.pragma.powerup.application.handler.IRestaurantHandler;
import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.InvalidRestaurantNameException;
import com.pragma.powerup.domain.exception.RestaurantAlreadyExistsException;
import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
import com.pragma.powerup.infrastructure.exceptionhandler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestaurantRestControllerTest {
    private static final String RESTAURANTS_URL = "/api/v1/restaurants";
    private static final String VALID_NAME = "Pizza 24";
    private static final String VALID_NIT = "900111222";
    private static final String VALID_PHONE = "+573001112233";

    @Mock
    private IRestaurantHandler restaurantHandler;

    @InjectMocks
    private RestaurantRestController restaurantRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void saveRestaurantWithValidRequestReturns201AndTheCreatedRestaurant() throws Exception {
        RestaurantResponseDto response = new RestaurantResponseDto();
        response.setId(1L);
        response.setName(VALID_NAME);
        response.setOwnerId(1L);
        when(restaurantHandler.saveRestaurant(any(RestaurantRequestDto.class))).thenReturn(response);

        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(VALID_NAME, VALID_NIT, VALID_PHONE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value(VALID_NAME))
                .andExpect(jsonPath("$.ownerId").value(1));
    }

    @Test
    void saveRestaurantWithNonNumericNitReturns400() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(VALID_NAME, "90A", VALID_PHONE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nit").value("must contain only digits"));

        verifyNoInteractions(restaurantHandler);
    }

    @Test
    void saveRestaurantWithPhoneLongerThan13CharactersReturns400() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(VALID_NAME, VALID_NIT, "+5730011122334")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.phone").exists());

        verifyNoInteractions(restaurantHandler);
    }

    @Test
    void saveRestaurantWithEmptyBodyReturns400ReportingEveryRequiredField() throws Exception {
        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.nit").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.phone").exists())
                .andExpect(jsonPath("$.logoUrl").exists())
                .andExpect(jsonPath("$.ownerId").exists());

        verifyNoInteractions(restaurantHandler);
    }

    @Test
    void saveRestaurantWhenNameIsInvalidReturns400WithTheDomainMessage() throws Exception {
        when(restaurantHandler.saveRestaurant(any(RestaurantRequestDto.class)))
                .thenThrow(new InvalidRestaurantNameException("The restaurant name cannot contain only numbers"));

        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson("12345", VALID_NIT, VALID_PHONE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The restaurant name cannot contain only numbers"));
    }

    @Test
    void saveRestaurantWhenUserIsNotAnOwnerReturns400WithTheDomainMessage() throws Exception {
        when(restaurantHandler.saveRestaurant(any(RestaurantRequestDto.class)))
                .thenThrow(new InvalidOwnerException("The user is not an owner"));

        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(VALID_NAME, VALID_NIT, VALID_PHONE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The user is not an owner"));
    }

    @Test
    void saveRestaurantWhenNitAlreadyExistsReturns409WithTheDomainMessage() throws Exception {
        when(restaurantHandler.saveRestaurant(any(RestaurantRequestDto.class)))
                .thenThrow(new RestaurantAlreadyExistsException("A restaurant with this NIT already exists"));

        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(VALID_NAME, VALID_NIT, VALID_PHONE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A restaurant with this NIT already exists"));
    }

    @Test
    void saveRestaurantWhenUsersServiceIsDownReturns503WithTheDomainMessage() throws Exception {
        when(restaurantHandler.saveRestaurant(any(RestaurantRequestDto.class)))
                .thenThrow(new UserServiceUnavailableException("The users service is not available"));

        mockMvc.perform(post(RESTAURANTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(VALID_NAME, VALID_NIT, VALID_PHONE)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The users service is not available"));
    }

    private String buildRequestJson(String name, String nit, String phone) {
        return """
                {
                  "name": "%s",
                  "nit": "%s",
                  "address": "Calle 1 # 2-3",
                  "phone": "%s",
                  "logoUrl": "https://logos.com/pizza.png",
                  "ownerId": 1
                }
                """.formatted(name, nit, phone);
    }

    @Test
    void listRestaurantsReturns200WithOnlyTheNameAndLogoOfEachRestaurant() throws Exception {
        RestaurantSummaryResponseDto summary = new RestaurantSummaryResponseDto();
        summary.setName("Arepas de la 70");
        summary.setLogoUrl("https://logos.com/arepas.png");
        PageResponseDto<RestaurantSummaryResponseDto> response =
                new PageResponseDto<>(List.of(summary), 0, 10, 1L, 1);
        when(restaurantHandler.listRestaurants(0, 10)).thenReturn(response);

        mockMvc.perform(get(RESTAURANTS_URL).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Arepas de la 70"))
                .andExpect(jsonPath("$.content[0].logoUrl").value("https://logos.com/arepas.png"))
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].nit").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listRestaurantsWithoutParametersUsesTheFirstPageAndTenElements() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL))
                .andExpect(status().isOk());

        verify(restaurantHandler).listRestaurants(0, 10);
    }

    @Test
    void listRestaurantsPassesThePageAndSizeOfTheUrlToTheHandler() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL).param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(restaurantHandler).listRestaurants(2, 5);
    }

    @Test
    void listRestaurantsWithInvalidPaginationReturns400WithTheDomainMessage() throws Exception {
        when(restaurantHandler.listRestaurants(0, 51))
                .thenThrow(new InvalidPaginationException("The page size must be between 1 and 50"));

        mockMvc.perform(get(RESTAURANTS_URL).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The page size must be between 1 and 50"));
    }

    @Test
    void listRestaurantsWithANonNumericSizeReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(get(RESTAURANTS_URL).param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'size'"));

        verifyNoInteractions(restaurantHandler);
    }
}
