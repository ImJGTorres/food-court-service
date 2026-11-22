package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.RestaurantEmployeeRequestDto;
import com.pragma.powerup.application.dto.response.RestaurantEmployeeResponseDto;
import com.pragma.powerup.application.handler.IRestaurantEmployeeHandler;
import com.pragma.powerup.domain.exception.EmployeeAlreadyAssignedException;
import com.pragma.powerup.domain.exception.InvalidEmployeeException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.exception.RestaurantNotFoundException;
import com.pragma.powerup.domain.exception.UserServiceUnavailableException;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestaurantEmployeeRestControllerTest {
    private static final String RESTAURANT_EMPLOYEES_URL = "/api/v1/restaurants/1/employees";
    private static final Long TOKEN_OWNER_ID = 10L;
    private static final String VALID_JSON = "{\"employeeId\": 15}";

    @Mock
    private IRestaurantEmployeeHandler restaurantEmployeeHandler;

    @InjectMocks
    private RestaurantEmployeeRestController restaurantEmployeeRestController;

    private MockMvc mockMvc;
    private Authentication ownerAuthentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantEmployeeRestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ownerAuthentication = new UsernamePasswordAuthenticationToken(TOKEN_OWNER_ID, null, List.of());
    }

    private void whenTheHandlerThrows(RuntimeException exception) {
        when(restaurantEmployeeHandler.assignEmployee(eq(1L), any(RestaurantEmployeeRequestDto.class),
                eq(TOKEN_OWNER_ID))).thenThrow(exception);
    }

    private ResultActions postValidLink() throws Exception {
        return mockMvc.perform(post(RESTAURANT_EMPLOYEES_URL)
                .principal(ownerAuthentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_JSON));
    }

    @Test
    void assignEmployeeReturns201WithTheLinkAndUsesTheOwnerOfTheToken() throws Exception {
        RestaurantEmployeeResponseDto response = new RestaurantEmployeeResponseDto();
        response.setEmployeeId(15L);
        response.setRestaurantId(1L);
        when(restaurantEmployeeHandler.assignEmployee(eq(1L), any(RestaurantEmployeeRequestDto.class),
                eq(TOKEN_OWNER_ID))).thenReturn(response);

        postValidLink()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(15))
                .andExpect(jsonPath("$.restaurantId").value(1));
    }

    @Test
    void assignEmployeeWithoutEmployeeIdReturns400() throws Exception {
        mockMvc.perform(post(RESTAURANT_EMPLOYEES_URL)
                        .principal(ownerAuthentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.employeeId").exists());

        verifyNoInteractions(restaurantEmployeeHandler);
    }

    @Test
    void assignEmployeeToAMissingRestaurantReturns404() throws Exception {
        whenTheHandlerThrows(new RestaurantNotFoundException("The requested restaurant does not exist"));

        postValidLink()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested restaurant does not exist"));
    }

    @Test
    void assignEmployeeToARestaurantOfAnotherOwnerReturns403() throws Exception {
        whenTheHandlerThrows(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its employees"));

        postValidLink()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owner of the restaurant can manage its employees"));
    }

    @Test
    void assignAUserThatIsNotAnEmployeeReturns400() throws Exception {
        whenTheHandlerThrows(new InvalidEmployeeException("The user is not an employee"));

        postValidLink()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The user is not an employee"));
    }

    @Test
    void assignAnEmployeeThatIsAlreadyAssignedReturns409() throws Exception {
        whenTheHandlerThrows(new EmployeeAlreadyAssignedException("The employee is already assigned to a restaurant"));

        postValidLink()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The employee is already assigned to a restaurant"));
    }

    @Test
    void assignEmployeeWhenUsersServiceIsDownReturns503() throws Exception {
        whenTheHandlerThrows(new UserServiceUnavailableException("The users service is not available"));

        postValidLink()
                .andExpect(status().isServiceUnavailable());
    }
}
