package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.response.RecommendationResponse;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.DietaryPreference;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.enums.WarningType;
import com.example.smartfoodordering.mapper.MenuMapper;
import com.example.smartfoodordering.repository.CustomerFoodProfileRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.service.AllergenDetectionEngine;
import com.example.smartfoodordering.service.RecommendationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule-based ranking. Items with a known allergen conflict are never recommended; the rest are scored on
 * dietary compliance, warnings, spice fit and favourite cuisines.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {
    private final CustomerFoodProfileRepository profileRepository;
    private final MenuItemRepository menuItemRepository;
    private final AllergenDetectionEngine engine;
    private final MenuMapper menuMapper;
    private final int maxResults;

    public RecommendationServiceImpl(CustomerFoodProfileRepository profileRepository,
                                     MenuItemRepository menuItemRepository, AllergenDetectionEngine engine,
                                     MenuMapper menuMapper,
                                     @Value("${app.recommendation.max-results:10}") int maxResults) {
        this.profileRepository = profileRepository;
        this.menuItemRepository = menuItemRepository;
        this.engine = engine;
        this.menuMapper = menuMapper;
        this.maxResults = maxResults;
    }

    @Override
    public List<RecommendationResponse> recommend(Long customerId) {
        CustomerFoodProfile profile = profileRepository.findDetailedByUserId(customerId)
                .orElseGet(CustomerFoodProfile::new);
        List<RecommendationResponse> ranked = new ArrayList<>();
        for (MenuItem item : menuItemRepository.findAllOrderableWithSafetyData()) {
            List<WarningResponse> warnings = engine.evaluate(profile, item);
            if (warnings.stream().anyMatch(w -> w.type() == WarningType.KNOWN_CONFLICT)) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            int score = 50;

            for (DietaryPreference pref : profile.getDietaryPreferences()) {
                if (item.getDietTags().contains(pref.getDietType())) {
                    score += pref.isStrict() ? 15 : 8;
                    reasons.add("Suits your " + pref.getDietType().name().toLowerCase().replace('_', '-') + " diet");
                }
            }
            for (WarningResponse w : warnings) {
                switch (w.type()) {
                    case POSSIBLE_CONFLICT -> score -= 25;
                    case DIETARY_MISMATCH -> score -= 20;
                    case INFORMATION_INCOMPLETE -> score -= 10;
                    default -> { }
                }
            }
            if (item.getSpiceLevel().ordinal() <= profile.getMaxSpiceLevel().ordinal()) {
                score += 10;
                reasons.add("Within your spice comfort level");
            } else {
                score -= 15;
            }
            boolean favourite = profile.getFavoriteCuisines().stream()
                    .anyMatch(c -> c.equalsIgnoreCase(item.getCuisine()));
            if (favourite) {
                score += 15;
                reasons.add("From a cuisine you like (" + item.getCuisine() + ")");
            }
            if (warnings.isEmpty() && item.isAllergenInfoComplete()) {
                score += 10;
                reasons.add("No allergen or dietary warnings");
            }
            ranked.add(new RecommendationResponse(menuMapper.toResponse(item), score, reasons));
        }
        return ranked.stream()
                .sorted(Comparator.comparingInt(RecommendationResponse::score).reversed()
                        .thenComparing(r -> r.item().name()))
                .limit(maxResults)
                .toList();
    }
}
