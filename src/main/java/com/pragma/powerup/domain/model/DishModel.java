package com.pragma.powerup.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DishModel {
    private Long id;
    private String name;
    private Integer price;
    private String description;
    private String imageUrl;
    private Boolean active;
    private CategoryModel category;
    private RestaurantModel restaurant;
}
