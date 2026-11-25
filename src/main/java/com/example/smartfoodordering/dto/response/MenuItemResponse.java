package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.SpiceLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record MenuItemResponse(Long id, Long restaurantId, String restaurantName, String name, String description,
                               BigDecimal price, String cuisine, SpiceLevel spiceLevel, boolean available,
                               boolean allergenInfoComplete, List<String> declaredAllergens,
                               List<String> mayContainAllergens, List<String> ingredients, Set<DietType> dietTags) {
}
