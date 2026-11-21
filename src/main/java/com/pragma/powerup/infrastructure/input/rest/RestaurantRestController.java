package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.RestaurantRequestDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.dto.response.RestaurantResponseDto;
import com.pragma.powerup.application.dto.response.RestaurantSummaryResponseDto;
import com.pragma.powerup.application.handler.IRestaurantHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RestaurantRestController {
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "10";

    private final IRestaurantHandler restaurantHandler;

    @Operation(summary = "Create a restaurant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Restaurant created",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = RestaurantResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data, invalid name or user is not an owner",
                content = @Content),
            @ApiResponse(responseCode = "409", description = "NIT already registered", content = @Content),
            @ApiResponse(responseCode = "503", description = "Users service not available", content = @Content)
    })
    @PostMapping(value = "/restaurants", consumes = "application/json", produces = "application/json")
    public ResponseEntity<RestaurantResponseDto> saveRestaurant(
            @Valid @RequestBody RestaurantRequestDto restaurantRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantHandler.saveRestaurant(restaurantRequestDto));
    }


    @Operation(summary = "List the restaurants in alphabetical order, page by page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of restaurants (name and logo only)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid page or size", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Only a client can list restaurants", content = @Content)
    })
    @GetMapping(value = "/restaurants", produces = "application/json")
    public ResponseEntity<PageResponseDto<RestaurantSummaryResponseDto>> listRestaurants(
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(restaurantHandler.listRestaurants(page, size));
    }

}
