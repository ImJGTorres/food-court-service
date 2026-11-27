package com.pragma.powerup.infrastructure.input.rest;

import com.pragma.powerup.application.dto.request.OrderDeliveryRequestDto;
import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.handler.IOrderHandler;
import com.pragma.powerup.domain.model.OrderStatus;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderRestController {
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "10";

    private final IOrderHandler orderHandler;

    @Operation(summary = "Place an order with dishes of one restaurant; it starts as PENDING")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created as PENDING",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid data, repeated dish, dish of another restaurant or disabled dish",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Only a client can place orders", content = @Content),
            @ApiResponse(responseCode = "404", description = "Restaurant or dish not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "The client already has an order in progress",
                    content = @Content)
    })
    @PostMapping(value = "/orders", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderHandler.createOrder(orderRequestDto, getUserId(authentication)));
    }

    @Operation(summary = "List the orders of the restaurant of the employee by status, from the oldest")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of orders with all their fields",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid status, page or size",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Only an employee assigned to a restaurant can list its orders", content = @Content)
    })
    @GetMapping(value = "/orders", produces = "application/json")
    public ResponseEntity<PageResponseDto<OrderResponseDto>> listOrdersByStatus(
            @RequestParam OrderStatus status,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            Authentication authentication) {
        return ResponseEntity.ok(orderHandler.listOrdersByStatus(getUserId(authentication), status, page, size));
    }

    @Operation(summary = "Assign the order to the employee of the token; it moves from PENDING to IN_PREPARATION")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order assigned and in preparation",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Not an employee, not assigned to a restaurant or the order is of another restaurant",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "The order is not pending or someone else changed it at the same time",
                    content = @Content)
    })
    @PatchMapping(value = "/orders/{orderId}/assign", produces = "application/json")
    public ResponseEntity<OrderResponseDto> assignOrder(@PathVariable Long orderId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(orderHandler.assignOrder(orderId, getUserId(authentication)));
    }

    @Operation(summary = "Mark the order as ready: generates the security PIN and sends it to the client by SMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order ready; the PIN was sent to the client",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Not an employee or not the employee assigned to the order", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "The order is not in preparation or changed at the same time",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "The client phone cannot receive SMS",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Messaging or users service not available",
                    content = @Content)
    })
    @PatchMapping(value = "/orders/{orderId}/ready", produces = "application/json")
    public ResponseEntity<OrderResponseDto> markOrderReady(@PathVariable Long orderId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(orderHandler.markOrderReady(orderId, getUserId(authentication)));
    }

    @Operation(summary = "Deliver a ready order: the employee must type the PIN the client received by SMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order delivered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "The PIN is missing or does not have 6 digits",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Not an employee, not the employee assigned to the order or incorrect PIN",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "The order is not ready or changed at the same time",
                    content = @Content),
            @ApiResponse(responseCode = "423", description = "Too many incorrect PIN attempts: the order is locked",
                    content = @Content)
    })
    @PatchMapping(value = "/orders/{orderId}/deliver", consumes = "application/json", produces = "application/json")
    public ResponseEntity<OrderResponseDto> deliverOrder(@PathVariable Long orderId,
                                                         @Valid @RequestBody OrderDeliveryRequestDto orderDeliveryRequestDto,
                                                         Authentication authentication) {
        return ResponseEntity.ok(orderHandler.deliverOrder(orderId, getUserId(authentication), orderDeliveryRequestDto));
    }

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
