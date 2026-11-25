package com.example.smartfoodordering.mapper;

import com.example.smartfoodordering.dto.response.AllergyResponse;
import com.example.smartfoodordering.dto.response.DietPrefResponse;
import com.example.smartfoodordering.dto.response.FoodProfileResponse;
import com.example.smartfoodordering.dto.response.IngredientResponse;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Must be called inside a transaction: it reads lazy collections. */
@Component
public class ProfileMapper {

    public FoodProfileResponse toResponse(CustomerFoodProfile p) {
        List<AllergyResponse> allergies = p.getAllergies().stream()
                .map(a -> new AllergyResponse(a.getId(), a.getAllergen().getCode(), a.getAllergen().getName(),
                        a.getSeverity(), a.getNotes()))
                .sorted(Comparator.comparing(AllergyResponse::allergenName))
                .toList();
        List<DietPrefResponse> diets = p.getDietaryPreferences().stream()
                .map(d -> new DietPrefResponse(d.getId(), d.getDietType(), d.isStrict()))
                .sorted(Comparator.comparing(DietPrefResponse::dietType))
                .toList();
        List<IngredientResponse> avoided = p.getAvoidedIngredients().stream()
                .map(i -> new IngredientResponse(i.getId(), i.getName()))
                .sorted(Comparator.comparing(IngredientResponse::name))
                .toList();
        List<String> cuisines = p.getFavoriteCuisines().stream().sorted().toList();
        return new FoodProfileResponse(p.getMaxSpiceLevel(), cuisines, avoided, allergies, diets, p.isConfirmedComplete());
    }
}
