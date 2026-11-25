package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.CustomerAllergy;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.DietaryPreference;
import com.example.smartfoodordering.entity.Ingredient;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.MenuItemAllergen;
import com.example.smartfoodordering.entity.MenuItemIngredient;
import com.example.smartfoodordering.enums.AllergenSource;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.WarningType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Pure safety rules, with no database access. Callers must pass entities whose lazy collections are loaded.
 *
 * <ul>
 *   <li>KNOWN_CONFLICT: the item declares an allergen the customer is allergic to (blocks ordering).</li>
 *   <li>POSSIBLE_CONFLICT: cross-contact is declared, or an ingredient commonly carries the allergen.</li>
 *   <li>DIETARY_MISMATCH: an ingredient breaks a diet or is on the customer's avoid list.</li>
 *   <li>INFORMATION_INCOMPLETE: the restaurant has not confirmed the ingredient/allergen data.</li>
 * </ul>
 */
@Component
public class AllergenDetectionEngine {

    public List<WarningResponse> evaluate(CustomerFoodProfile profile, MenuItem item) {
        List<WarningResponse> warnings = new ArrayList<>();

        Map<String, MenuItemAllergen> declared = new HashMap<>();
        Map<String, MenuItemAllergen> mayContain = new HashMap<>();
        for (MenuItemAllergen ma : item.getAllergens()) {
            String code = ma.getAllergen().getCode();
            if (ma.getSource() == AllergenSource.DECLARED) {
                declared.put(code, ma);
            } else {
                mayContain.put(code, ma);
            }
        }
        Set<Ingredient> ingredients = item.getIngredients().stream()
                .map(MenuItemIngredient::getIngredient).collect(Collectors.toSet());

        for (CustomerAllergy allergy : profile.getAllergies()) {
            String code = allergy.getAllergen().getCode();
            String name = allergy.getAllergen().getName();
            AllergySeverity severity = allergy.getSeverity();
            if (declared.containsKey(code)) {
                warnings.add(warning(item, WarningType.KNOWN_CONFLICT, name, severity,
                        "Contains " + name + ", which is in your allergy list."));
            } else if (mayContain.containsKey(code)) {
                warnings.add(warning(item, WarningType.POSSIBLE_CONFLICT, name, severity,
                        "May contain " + name + " (cross-contact risk)."));
            } else {
                Optional<Ingredient> carrier = ingredients.stream()
                        .filter(i -> i.getCommonAllergens().stream().anyMatch(a -> code.equals(a.getCode())))
                        .findFirst();
                carrier.ifPresent(i -> warnings.add(warning(item, WarningType.POSSIBLE_CONFLICT, name, severity,
                        i.getName() + " commonly contains " + name + ", but the restaurant has not declared it.")));
            }
        }

        for (Ingredient avoided : profile.getAvoidedIngredients()) {
            boolean present = ingredients.stream().anyMatch(i -> sameIngredient(i, avoided));
            if (present) {
                warnings.add(warning(item, WarningType.DIETARY_MISMATCH, avoided.getName(), null,
                        "Contains " + avoided.getName() + ", which you chose to avoid."));
            }
        }

        for (DietaryPreference pref : profile.getDietaryPreferences()) {
            if (item.getDietTags().contains(pref.getDietType())) {
                continue;
            }
            Optional<Ingredient> violator = ingredients.stream()
                    .filter(i -> i.getIncompatibleDiets().contains(pref.getDietType())).findFirst();
            violator.ifPresent(i -> warnings.add(warning(item, WarningType.DIETARY_MISMATCH,
                    pref.getDietType().name(), null,
                    i.getName() + " does not fit your " + pref.getDietType().name().toLowerCase().replace('_', '-')
                            + " diet" + (pref.isStrict() ? " (strict)." : "."))));
        }

        boolean hasConcerns = !profile.getAllergies().isEmpty() || !profile.getDietaryPreferences().isEmpty()
                || !profile.getAvoidedIngredients().isEmpty();
        if (hasConcerns && (!item.isAllergenInfoComplete() || item.getIngredients().isEmpty())) {
            warnings.add(warning(item, WarningType.INFORMATION_INCOMPLETE, null, null,
                    "The restaurant has not confirmed the full ingredient and allergen details for this item."));
        }
        return warnings;
    }

    private WarningResponse warning(MenuItem item, WarningType type, String subject, AllergySeverity severity,
                                    String message) {
        return new WarningResponse(item.getId(), item.getName(), type, subject, severity, message,
                type == WarningType.KNOWN_CONFLICT);
    }

    private boolean sameIngredient(Ingredient a, Ingredient b) {
        if (a.getId() != null && a.getId().equals(b.getId())) {
            return true;
        }
        return a.getName() != null && a.getName().equalsIgnoreCase(b.getName());
    }
}
