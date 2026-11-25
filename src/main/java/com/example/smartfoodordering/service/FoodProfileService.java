package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.AllergyRequest;
import com.example.smartfoodordering.dto.request.DietRequest;
import com.example.smartfoodordering.dto.request.PreferencesRequest;
import com.example.smartfoodordering.dto.response.FoodProfileResponse;

/** Every method works only on the profile of the given user id, which controllers take from the session. */
public interface FoodProfileService {
    FoodProfileResponse getProfile(Long userId);

    void updatePreferences(Long userId, PreferencesRequest request);

    void addAllergy(Long userId, AllergyRequest request);

    void removeAllergy(Long userId, Long allergyId);

    void addDiet(Long userId, DietRequest request);

    void removeDiet(Long userId, Long dietId);

    void addAvoidedIngredient(Long userId, Long ingredientId);

    void removeAvoidedIngredient(Long userId, Long ingredientId);

    /** Plain-text summary stored with an order so the history stays accurate. */
    String buildSnapshot(Long userId);
}
