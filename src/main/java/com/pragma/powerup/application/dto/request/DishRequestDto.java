package com.pragma.powerup.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DishRequestDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Positive
    private Integer price;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String imageUrl;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long restaurantId;

    @NotNull
    private Long ownerId;

}
