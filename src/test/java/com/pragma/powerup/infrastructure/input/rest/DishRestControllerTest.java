package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.request.DishStatusRequestDto;
import com.pragma.powerup.application.dto.request.DishUpdateRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.dto.response.DishSummaryResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.handler.IDishHandler;
import com.pragma.powerup.domain.exception.CategoryNotFoundException;
import com.pragma.powerup.domain.exception.DishAlreadyExistsException;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.infrastructure.exceptionhandler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DishRestControllerTest {
    private static final String DISHES_URL = "/api/v1/dishes";
    private static final String DISH_URL = "/api/v1/dishes/7";
    private static final String DISH_STATUS_URL = "/api/v1/dishes/7/status";
    private static final Long TOKEN_USER_ID = 1L;
    private static final String RESTAURANT_DISHES_URL = "/api/v1/restaurants/1/dishes";

    @Mock
    private IDishHandler dishHandler;

    @InjectMocks
    private DishRestController dishRestController;

    private MockMvc mockMvc;
    private Authentication ownerAuthentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dishRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ownerAuthentication = new UsernamePasswordAuthenticationToken(TOKEN_USER_ID, null, List.of());
    }

    @Test
    void saveDishWithValidRequestReturns201AndTheCreatedDish() throws Exception {
        DishResponseDto response = new DishResponseDto();
        response.setId(1L);
        response.setName("Hamburguesa");
        response.setActive(true);
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID))).thenReturn(response);

        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hamburguesa"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void saveDishTakesTheOwnerIdFromTheAuthenticatedUserAndIgnoresOneSentInTheBody() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID))).thenReturn(new DishResponseDto());

        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000).replace("\"restaurantId\": 1",
                                "\"restaurantId\": 1, \"ownerId\": 99")))
                .andExpect(status().isCreated());

        verify(dishHandler).saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID));
    }

    @Test
    void saveDishWithZeroPriceReturns400() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWithNegativePriceReturns400() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(-5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWithEmptyBodyReturns400ReportingEveryRequiredField() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.imageUrl").exists())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.restaurantId").exists())
                .andExpect(jsonPath("$.ownerId").doesNotExist());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWhenRestaurantDoesNotExistReturns404WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new RestaurantNotFoundException("The requested restaurant does not exist"));

        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested restaurant does not exist"));
    }

    @Test
    void saveDishWhenCategoryDoesNotExistReturns404WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new CategoryNotFoundException("The requested category does not exist"));

        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested category does not exist"));
    }

    @Test
    void saveDishWhenCallerIsNotTheOwnerReturns403WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its dishes"));

        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owner of the restaurant can manage its dishes"));
    }

    @Test
    void saveDishWhenTheDishAlreadyExistsReturns409WithTheDomainMessage() throws Exception {
        when(dishHandler.saveDish(any(DishRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new DishAlreadyExistsException("A dish with this name already exists in the restaurant"));

        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson(15000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A dish with this name already exists in the restaurant"));
    }

    @Test
    void updateDishWithValidRequestReturns200AndTheUpdatedDish() throws Exception {
        DishResponseDto response = new DishResponseDto();
        response.setId(7L);
        response.setPrice(18000);
        response.setDescription("Con queso y tocineta");
        when(dishHandler.updateDish(eq(7L), any(DishUpdateRequestDto.class), eq(TOKEN_USER_ID)))
                .thenReturn(response);

        mockMvc.perform(patch(DISH_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(18000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.price").value(18000))
                .andExpect(jsonPath("$.description").value("Con queso y tocineta"));
    }

    @Test
    void updateDishWithZeroPriceReturns400() throws Exception {
        mockMvc.perform(patch(DISH_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishWithNegativePriceReturns400() throws Exception {
        mockMvc.perform(patch(DISH_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(-5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishWithEmptyBodyReturns400ReportingEveryRequiredField() throws Exception {
        mockMvc.perform(patch(DISH_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.ownerId").doesNotExist());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishWithNonNumericIdReturns400WithoutStackTrace() throws Exception {
        mockMvc.perform(patch("/api/v1/dishes/abc")
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(18000)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishWhenDishDoesNotExistReturns404WithTheDomainMessage() throws Exception {
        when(dishHandler.updateDish(eq(7L), any(DishUpdateRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new DishNotFoundException("The requested dish does not exist"));

        mockMvc.perform(patch(DISH_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(18000)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested dish does not exist"));
    }

    @Test
    void updateDishWhenCallerIsNotTheOwnerReturns403WithTheDomainMessage() throws Exception {
        when(dishHandler.updateDish(eq(7L), any(DishUpdateRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its dishes"));

        mockMvc.perform(patch(DISH_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(18000)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owner of the restaurant can manage its dishes"));
    }

    @Test
    void updateDishStatusWithValidRequestReturns200AndTheDishWithItsNewStatus() throws Exception {
        DishResponseDto response = new DishResponseDto();
        response.setId(7L);
        response.setActive(false);
        when(dishHandler.updateDishStatus(eq(7L), any(DishStatusRequestDto.class), eq(TOKEN_USER_ID)))
                .thenReturn(response);

        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateDishStatusGivesTheHandlerTheUserIdOfTheTokenAndTheRequestedStatus() throws Exception {
        when(dishHandler.updateDishStatus(eq(7L), any(DishStatusRequestDto.class), eq(TOKEN_USER_ID)))
                .thenReturn(new DishResponseDto());

        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isOk());

        ArgumentCaptor<DishStatusRequestDto> captor = ArgumentCaptor.forClass(DishStatusRequestDto.class);
        verify(dishHandler).updateDishStatus(eq(7L), captor.capture(), eq(TOKEN_USER_ID));
        assertTrue(captor.getValue().getActive());
    }

    @Test
    void updateDishStatusWithoutTheActiveFieldReturns400AndDoesNotDisableTheDish() throws Exception {
        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.active").exists());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishStatusWithANonBooleanValueReturns400() throws Exception {
        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": \"maybe\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishStatusWithNonNumericIdReturns400WithoutStackTrace() throws Exception {
        mockMvc.perform(patch("/api/v1/dishes/abc/status")
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

        verifyNoInteractions(dishHandler);
    }

    @Test
    void updateDishStatusWhenDishDoesNotExistReturns404WithTheDomainMessage() throws Exception {
        when(dishHandler.updateDishStatus(eq(7L), any(DishStatusRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new DishNotFoundException("The requested dish does not exist"));

        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested dish does not exist"));
    }

    @Test
    void updateDishStatusWhenCallerIsNotTheOwnerReturns403WithTheDomainMessage() throws Exception {
        when(dishHandler.updateDishStatus(eq(7L), any(DishStatusRequestDto.class), eq(TOKEN_USER_ID)))
                .thenThrow(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its dishes"));

        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owner of the restaurant can manage its dishes"));
    }

    @Test
    void updateDishStatusWithAWrongTypeReturns400WithAShortMessageAndNoInternalDetails() throws Exception {
        mockMvc.perform(patch(DISH_STATUS_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": \"maybe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verifyNoInteractions(dishHandler);
    }

    @Test
    void saveDishWithBrokenJsonReturns400WithAShortMessageAndNoInternalDetails() throws Exception {
        mockMvc.perform(post(DISHES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verifyNoInteractions(dishHandler);
    }

    private String buildUpdateJson(int price) {
        return """
                {
                  "price": %d,
                  "description": "Con queso y tocineta"
                }
                """.formatted(price);
    }

    private String buildRequestJson(int price) {
        return """
                {
                  "name": "Hamburguesa",
                  "price": %d,
                  "description": "Con queso y tocineta",
                  "imageUrl": "https://img.com/hamburguesa.png",
                  "categoryId": 1,
                  "restaurantId": 1
                }
                """.formatted(price);
    }

    @Test
    void listDishesReturns200WithTheFieldsTheClientNeedsToChooseADish() throws Exception {
        DishSummaryResponseDto summary = new DishSummaryResponseDto();
        summary.setId(4L);
        summary.setName("Hamburguesa");
        summary.setPrice(15000);
        summary.setDescription("Con queso");
        summary.setImageUrl("https://img.com/burger.png");
        summary.setCategoryName("ENTRADA");
        when(dishHandler.listDishes(1L, null, 0, 10))
                .thenReturn(new PageResponseDto<>(List.of(summary), 0, 10, 1L, 1));

        mockMvc.perform(get(RESTAURANT_DISHES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.content[0].name").value("Hamburguesa"))
                .andExpect(jsonPath("$.content[0].price").value(15000))
                .andExpect(jsonPath("$.content[0].description").value("Con queso"))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://img.com/burger.png"))
                .andExpect(jsonPath("$.content[0].categoryName").value("ENTRADA"))
                .andExpect(jsonPath("$.content[0].active").doesNotExist())
                .andExpect(jsonPath("$.content[0].restaurantId").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listDishesWithoutParametersHasNoCategoryFilterAndUsesTheFirstPageOfTen() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL))
                .andExpect(status().isOk());

        verify(dishHandler).listDishes(eq(1L), isNull(), eq(0), eq(10));
    }

    @Test
    void listDishesPassesTheCategoryPageAndSizeOfTheUrlToTheHandler() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL)
                        .param("categoryId", "3").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(dishHandler).listDishes(1L, 3L, 2, 5);
    }

    @Test
    void listDishesOfAMissingRestaurantReturns404() throws Exception {
        when(dishHandler.listDishes(1L, null, 0, 10))
                .thenThrow(new RestaurantNotFoundException("The requested restaurant does not exist"));

        mockMvc.perform(get(RESTAURANT_DISHES_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested restaurant does not exist"));
    }

    @Test
    void listDishesWithAMissingCategoryReturns404() throws Exception {
        when(dishHandler.listDishes(1L, 99L, 0, 10))
                .thenThrow(new CategoryNotFoundException("The requested category does not exist"));

        mockMvc.perform(get(RESTAURANT_DISHES_URL).param("categoryId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested category does not exist"));
    }

    @Test
    void listDishesWithInvalidPaginationReturns400WithTheDomainMessage() throws Exception {
        when(dishHandler.listDishes(1L, null, 0, 51))
                .thenThrow(new InvalidPaginationException("The page size must be between 1 and 50"));

        mockMvc.perform(get(RESTAURANT_DISHES_URL).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The page size must be between 1 and 50"));
    }

    @Test
    void listDishesWithANonNumericCategoryReturns400WithAShortMessage() throws Exception {
        mockMvc.perform(get(RESTAURANT_DISHES_URL).param("categoryId", "postres"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'categoryId'"));

        verifyNoInteractions(dishHandler);
    }
}
