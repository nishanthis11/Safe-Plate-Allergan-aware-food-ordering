package com.example.smartfoodordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.smartfoodordering.dto.response.RecommendationResponse;
import com.example.smartfoodordering.entity.Allergen;
import com.example.smartfoodordering.entity.CustomerAllergy;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.MenuItemAllergen;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.enums.AllergenSource;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.SpiceLevel;
import com.example.smartfoodordering.mapper.MenuMapper;
import com.example.smartfoodordering.repository.CustomerFoodProfileRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.service.impl.RecommendationServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {
    @Mock
    private CustomerFoodProfileRepository profileRepository;
    @Mock
    private MenuItemRepository menuItemRepository;

    private RecommendationServiceImpl service;
    private Restaurant restaurant;
    private Allergen peanut;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(profileRepository, menuItemRepository,
                new AllergenDetectionEngine(), new MenuMapper(), 10);
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Test Kitchen");
        peanut = new Allergen("PEANUT", "Peanut", null);
    }

    private MenuItem dish(long id, String name, String cuisine, SpiceLevel spice) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName(name);
        m.setCuisine(cuisine);
        m.setPrice(new BigDecimal("100.00"));
        m.setSpiceLevel(spice);
        m.setRestaurant(restaurant);
        m.setAllergenInfoComplete(true);
        return m;
    }

    @Test
    void neverRecommendsItemsWithKnownAllergenConflict() {
        CustomerFoodProfile profile = new CustomerFoodProfile();
        CustomerAllergy allergy = new CustomerAllergy();
        allergy.setAllergen(peanut);
        allergy.setSeverity(AllergySeverity.SEVERE);
        profile.getAllergies().add(allergy);

        MenuItem unsafe = dish(1, "Satay", "Thai", SpiceLevel.MILD);
        unsafe.getAllergens().add(new MenuItemAllergen(unsafe, peanut, AllergenSource.DECLARED));
        MenuItem safe = dish(2, "Salad", "Healthy", SpiceLevel.NONE);

        when(profileRepository.findDetailedByUserId(7L)).thenReturn(Optional.of(profile));
        when(menuItemRepository.findAllOrderableWithSafetyData()).thenReturn(List.of(unsafe, safe));

        List<RecommendationResponse> result = service.recommend(7L);

        assertThat(result).extracting(r -> r.item().name()).containsExactly("Salad");
    }

    @Test
    void ranksFavouriteCuisineAndSpiceFitHigher() {
        CustomerFoodProfile profile = new CustomerFoodProfile();
        profile.setMaxSpiceLevel(SpiceLevel.MILD);
        profile.getFavoriteCuisines().add("Thai");

        MenuItem thaiMild = dish(1, "Pad Thai", "Thai", SpiceLevel.MILD);
        MenuItem hotCurry = dish(2, "Hot Curry", "North Indian", SpiceLevel.EXTRA_HOT);

        when(profileRepository.findDetailedByUserId(7L)).thenReturn(Optional.of(profile));
        when(menuItemRepository.findAllOrderableWithSafetyData()).thenReturn(List.of(hotCurry, thaiMild));

        List<RecommendationResponse> result = service.recommend(7L);

        assertThat(result).extracting(r -> r.item().name()).containsExactly("Pad Thai", "Hot Curry");
        assertThat(result.get(0).score()).isGreaterThan(result.get(1).score());
    }
}
