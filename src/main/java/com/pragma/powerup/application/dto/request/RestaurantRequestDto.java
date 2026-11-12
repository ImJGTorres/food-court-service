package com.pragma.powerup.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RestaurantRequestDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "\\d+", message = "must contain only digits")
    private String nit;

    @NotBlank
    @Size(max = 150)
    private String address;

    @NotBlank
    @Size(max = 13)
    @Pattern(regexp = "\\+?\\d+", message = "must contain only digits and an optional leading +")
    private String phone;

    @NotBlank
    @Size(max = 255)
    private String logoUrl;

    @NotNull
    private Long ownerId;
}
