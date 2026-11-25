package com.example.smartfoodordering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.Allergen;
import com.example.smartfoodordering.entity.CustomerAllergy;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.DietaryPreference;
import com.example.smartfoodordering.entity.Ingredient;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.MenuItemAllergen;
import com.example.smartfoodordering.entity.MenuItemIngredient;
import com.example.smartfoodordering.enums.AllergenSource;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.IngredientCategory;
import com.example.smartfoodordering.enums.WarningType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AllergenDetectionEngineTest {
    private final AllergenDetectionEngine engine = new AllergenDetectionEngine();

    private Allergen peanut;
    private Allergen milk;
    private CustomerFoodProfile profile;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        peanut = new Allergen("PEANUT", "Peanut", null);
        milk = new Allergen("MILK", "Milk", null);
        profile = new CustomerFoodProfile();
        item = new MenuItem();
        item.setId(1L);
        item.setName("Test dish");
        item.setAllergenInfoComplete(true);
    }

    private void allergicTo(Allergen allergen) {
        CustomerAllergy allergy = new CustomerAllergy();
        allergy.setProfile(profile);
        allergy.setAllergen(allergen);
        allergy.setSeverity(AllergySeverity.SEVERE);
        profile.getAllergies().add(allergy);
    }

    private Ingredient ingredient(String name) {
        Ingredient ingredient = new Ingredient(name, IngredientCategory.OTHER);
        item.getIngredients().add(new MenuItemIngredient(item, ingredient));
        return ingredient;
    }

    private void declare(Allergen allergen, AllergenSource source) {
        item.getAllergens().add(new MenuItemAllergen(item, allergen, source));
    }

    @Test
    void declaredAllergenIsKnownBlockingConflict() {
        allergicTo(peanut);
        ingredient("Peanut sauce");
        declare(peanut, AllergenSource.DECLARED);

        List<WarningResponse> warnings = engine.evaluate(profile, item);

        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).type()).isEqualTo(WarningType.KNOWN_CONFLICT);
        assertThat(warnings.get(0).blocking()).isTrue();
        assertThat(warnings.get(0).severity()).isEqualTo(AllergySeverity.SEVERE);
    }

    @Test
    void crossContactIsPossibleConflictAndNotBlocking() {
        allergicTo(peanut);
        ingredient("Noodles");
        declare(peanut, AllergenSource.MAY_CONTAIN);

        List<WarningResponse> warnings = engine.evaluate(profile, item);

        assertThat(warnings).extracting(WarningResponse::type).containsExactly(WarningType.POSSIBLE_CONFLICT);
        assertThat(warnings.get(0).blocking()).isFalse();
    }

    @Test
    void undeclaredAllergenCarriedByIngredientIsPossibleConflict() {
        allergicTo(peanut);
        Ingredient sauce = ingredient("Satay sauce");
        sauce.getCommonAllergens().add(peanut);

        List<WarningResponse> warnings = engine.evaluate(profile, item);

        assertThat(warnings).extracting(WarningResponse::type).containsExactly(WarningType.POSSIBLE_CONFLICT);
        assertThat(warnings.get(0).message()).contains("Satay sauce");
    }

    @Test
    void incompatibleIngredientCausesDietaryMismatchUnlessItemIsTagged() {
        profile.getDietaryPreferences().add(new DietaryPreference(profile, DietType.VEGAN, true));
        Ingredient paneer = ingredient("Paneer");
        paneer.getIncompatibleDiets().add(DietType.VEGAN);

        assertThat(engine.evaluate(profile, item)).extracting(WarningResponse::type)
                .containsExactly(WarningType.DIETARY_MISMATCH);

        item.getDietTags().add(DietType.VEGAN);
        assertThat(engine.evaluate(profile, item)).isEmpty();
    }

    @Test
    void avoidedIngredientCausesDietaryMismatch() {
        Ingredient mushroom = new Ingredient("Mushroom", IngredientCategory.VEGETABLE);
        profile.getAvoidedIngredients().add(mushroom);
        ingredient("mushroom");

        assertThat(engine.evaluate(profile, item)).extracting(WarningResponse::type)
                .containsExactly(WarningType.DIETARY_MISMATCH);
    }

    @Test
    void incompleteItemWarnsOnlyWhenProfileHasConcerns() {
        item.setAllergenInfoComplete(false);
        assertThat(engine.evaluate(profile, item)).isEmpty();

        allergicTo(milk);
        assertThat(engine.evaluate(profile, item)).extracting(WarningResponse::type)
                .contains(WarningType.INFORMATION_INCOMPLETE);
    }

    @Test
    void safeItemProducesNoWarnings() {
        allergicTo(peanut);
        ingredient("Rice");
        declare(milk, AllergenSource.DECLARED);

        assertThat(engine.evaluate(profile, item)).isEmpty();
    }
}
