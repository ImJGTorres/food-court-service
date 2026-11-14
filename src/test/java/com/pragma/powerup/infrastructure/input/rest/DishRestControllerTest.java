package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.handler.IDishHandler;
import com.pragma.powerup.domain.exception.CategoryNotFoundException;
import com.pragma.powerup.domain.exception.DishAlreadyExistsException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DishRestControllerTest {
    private static final String DISHES_URL = "/api/v1/dishes";

    @Mock
    private IDishHandler dishHandler;

    @InjectMocks
    private DishRestController dishRestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dishRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void saveDishWithValidRequestReturns201AndTheCreatedDish() throws Exception {
        DishResponseDto response = new DishResponseDto();
        response.setId(1L);
        response.setName("Hamburguesa");
        response.setActive(true);
        when(dishHandler.saveDish(any(DishRequestDto.class))).thenReturn(response);

        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hamburguesa"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void saveDishWithZeroPriceReturns400() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWithNegativePriceReturns400() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(-5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWithEmptyBodyReturns400ReportingEveryRequiredField() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.imageUrl").exists())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.restaurantId").exists())
                .andExpect(jsonPath("$.ownerId").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWhenRestaurantDoesNotExistReturns404WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class)))
                .thenThrow(new RestaurantNotFoundException("The requested restaurant does not exist"));

        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested restaurant does not exist"));
    }

    @Test
    void saveDishWhenCategoryDoesNotExistReturns404WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class)))
                .thenThrow(new CategoryNotFoundException("The requested category does not exist"));

        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested category does not exist"));
    }

    @Test
    void saveDishWhenCallerIsNotTheOwnerReturns403WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class)))
                .thenThrow(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its dishes"));

        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owner of the restaurant can manage its dishes"));
    }

    @Test
    void saveDishWhenTheDishAlreadyExistsReturns409WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class)))
                .thenThrow(new DishAlreadyExistsException("A dish with this name already exists in the restaurant"));

        mockMvc.perform(post(DISHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A dish with this name already exists in the restaurant"));
    }

    private String buildRequestJson(int price) {
        return """
                {
                  "name": "Hamburguesa",
                  "price": %d,
                  "description": "Con queso y tocineta",
                  "imageUrl": "https://img.com/hamburguesa.png",
                  "categoryId": 1,
                  "restaurantId": 1,
                  "ownerId": 1
                }
                """.formatted(price);
    }
}
