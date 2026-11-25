package com.example.smartfoodordering.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartAddRequest {
    @NotNull
    private Long menuItemId;

    @Min(1) @Max(20)
    private int quantity = 1;

    @Size(max = 250)
    private String specialInstructions;

    /** Only used to send the browser back to the menu page after adding. */
    private Long restaurantId;
}
