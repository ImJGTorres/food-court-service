package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.RestaurantEmployeeRequestDto;
import com.pragma.powerup.application.dto.response.RestaurantEmployeeResponseDto;
import com.pragma.powerup.application.handler.IRestaurantEmployeeHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RestaurantEmployeeRestController {
    private final IRestaurantEmployeeHandler restaurantEmployeeHandler;

    @Operation(summary = "Assign an employee to the restaurant of the owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee assigned to the restaurant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RestaurantEmployeeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data or the user is not an employee",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Only the owner of the restaurant can assign employees",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Restaurant not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "The employee is already assigned to a restaurant",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Users service not available", content = @Content)
    })
    @PostMapping(value = "/restaurants/{restaurantId}/employees", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<RestaurantEmployeeResponseDto> assignEmployee(
            @PathVariable Long restaurantId,
            @Valid @RequestBody RestaurantEmployeeRequestDto requestDto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantEmployeeHandler.assignEmployee(restaurantId, requestDto, getUserId(authentication)));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
