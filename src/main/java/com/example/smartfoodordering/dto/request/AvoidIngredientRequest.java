package com.example.smartfoodordering.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvoidIngredientRequest {
    @NotNull
    private Long ingredientId;
}
