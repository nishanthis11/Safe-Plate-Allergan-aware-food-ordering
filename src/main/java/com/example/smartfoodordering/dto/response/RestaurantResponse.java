package com.example.smartfoodordering.dto.response;

import java.math.BigDecimal;

public record RestaurantResponse(Long id, String name, String cuisine, String description, String address,
                                 BigDecimal deliveryFee, boolean active) {
}
