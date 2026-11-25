package com.example.smartfoodordering.dto.response;

import com.example.smartfoodordering.enums.SpiceLevel;
import java.util.List;

public record FoodProfileResponse(SpiceLevel maxSpiceLevel,
                                  List<String> favoriteCuisines,
                                  List<IngredientResponse> avoidedIngredients,
                                  List<AllergyResponse> allergies,
                                  List<DietPrefResponse> dietaryPreferences,
                                  boolean confirmedComplete) {
}
