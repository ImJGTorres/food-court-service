package com.pragma.powerup.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DishUpdateRequestDto {

    @NotNull
    @Positive
    private Integer price;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    private Long ownerId;
}
