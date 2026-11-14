package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.handler.IDishHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DishRestController {
    private final IDishHandler dishHandler;

    @Operation(summary = "Create a dish")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dish created",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = DishResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content),
            @ApiResponse(responseCode = "403", description = "Only the restaurant owner can create dishes",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Restaurant or category not found",
                    content = @Content)
    })
    @PostMapping(value = "/dishes", consumes = "application/json", produces = "application/json")
    public ResponseEntity<DishResponseDto> saveDish(@Valid @RequestBody DishRequestDto dishRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dishHandler.saveDish(dishRequestDto));
    }
}
