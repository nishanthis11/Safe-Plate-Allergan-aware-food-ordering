package com.example.smartfoodordering.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantRequest {
    @NotBlank @Size(max = 120)
    private String name;

    @NotBlank @Size(max = 60)
    private String cuisine;

    @Size(max = 600)
    private String description;

    @Size(max = 250)
    private String address;

    @NotNull @DecimalMin("0.00")
    private BigDecimal deliveryFee = new BigDecimal("30.00");
}
