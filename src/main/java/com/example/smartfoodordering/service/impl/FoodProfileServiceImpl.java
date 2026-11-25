package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.request.AllergyRequest;
import com.example.smartfoodordering.dto.request.DietRequest;
import com.example.smartfoodordering.dto.request.PreferencesRequest;
import com.example.smartfoodordering.dto.response.FoodProfileResponse;
import com.example.smartfoodordering.entity.Allergen;
import com.example.smartfoodordering.entity.CustomerAllergy;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.DietaryPreference;
import com.example.smartfoodordering.entity.Ingredient;
import com.example.smartfoodordering.entity.User;
import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.mapper.ProfileMapper;
import com.example.smartfoodordering.repository.AllergenRepository;
import com.example.smartfoodordering.repository.CustomerFoodProfileRepository;
import com.example.smartfoodordering.repository.IngredientRepository;
import com.example.smartfoodordering.repository.UserRepository;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.service.FoodProfileService;
import com.example.smartfoodordering.util.TextUtil;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FoodProfileServiceImpl implements FoodProfileService {
    private final CustomerFoodProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final AllergenRepository allergenRepository;
    private final IngredientRepository ingredientRepository;
    private final ProfileMapper mapper;
    private final AuditService auditService;

    public FoodProfileServiceImpl(CustomerFoodProfileRepository profileRepository, UserRepository userRepository,
                                  AllergenRepository allergenRepository, IngredientRepository ingredientRepository,
                                  ProfileMapper mapper, AuditService auditService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Override
    public FoodProfileResponse getProfile(Long userId) {
        return mapper.toResponse(loadOrCreate(userId));
    }

    @Override
    public void updatePreferences(Long userId, PreferencesRequest request) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        profile.setMaxSpiceLevel(request.getMaxSpiceLevel());
        profile.getFavoriteCuisines().clear();
        profile.getFavoriteCuisines().addAll(TextUtil.splitCsv(request.getFavoriteCuisines()));
        profile.setConfirmedComplete(request.isConfirmedComplete());
        profileRepository.save(profile);
        audit(profile, AuditAction.PREFERENCE_UPDATED, "Taste preferences updated");
    }

    @Override
    public void addAllergy(Long userId, AllergyRequest request) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        Allergen allergen = allergenRepository.findById(request.getAllergenId())
                .orElseThrow(() -> new ResourceNotFoundException("Allergen not found"));
        Optional<CustomerAllergy> existing = profile.getAllergies().stream()
                .filter(a -> a.getAllergen().getId().equals(allergen.getId())).findFirst();
        if (existing.isPresent()) {
            existing.get().setSeverity(request.getSeverity());
            existing.get().setNotes(request.getNotes());
        } else {
            CustomerAllergy allergy = new CustomerAllergy();
            allergy.setProfile(profile);
            allergy.setAllergen(allergen);
            allergy.setSeverity(request.getSeverity());
            allergy.setNotes(request.getNotes());
            profile.getAllergies().add(allergy);
        }
        profileRepository.save(profile);
        audit(profile, AuditAction.ALLERGY_ADDED, "Allergy saved: " + allergen.getCode());
    }

    @Override
    public void removeAllergy(Long userId, Long allergyId) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        boolean removed = profile.getAllergies().removeIf(a -> a.getId().equals(allergyId));
        if (!removed) {
            throw new ResourceNotFoundException("Allergy not found");
        }
        profileRepository.save(profile);
        audit(profile, AuditAction.ALLERGY_REMOVED, "Allergy removed");
    }

    @Override
    public void addDiet(Long userId, DietRequest request) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        Optional<DietaryPreference> existing = profile.getDietaryPreferences().stream()
                .filter(d -> d.getDietType() == request.getDietType()).findFirst();
        if (existing.isPresent()) {
            existing.get().setStrict(request.isStrict());
        } else {
            profile.getDietaryPreferences().add(new DietaryPreference(profile, request.getDietType(), request.isStrict()));
        }
        profileRepository.save(profile);
        audit(profile, AuditAction.PREFERENCE_UPDATED, "Dietary preference saved: " + request.getDietType());
    }

    @Override
    public void removeDiet(Long userId, Long dietId) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        boolean removed = profile.getDietaryPreferences().removeIf(d -> d.getId().equals(dietId));
        if (!removed) {
            throw new ResourceNotFoundException("Dietary preference not found");
        }
        profileRepository.save(profile);
        audit(profile, AuditAction.PREFERENCE_UPDATED, "Dietary preference removed");
    }

    @Override
    public void addAvoidedIngredient(Long userId, Long ingredientId) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));
        profile.getAvoidedIngredients().add(ingredient);
        profileRepository.save(profile);
        audit(profile, AuditAction.PREFERENCE_UPDATED, "Avoided ingredient added: " + ingredient.getName());
    }

    @Override
    public void removeAvoidedIngredient(Long userId, Long ingredientId) {
        CustomerFoodProfile profile = loadOrCreate(userId);
        boolean removed = profile.getAvoidedIngredients().removeIf(i -> i.getId().equals(ingredientId));
        if (!removed) {
            throw new ResourceNotFoundException("Ingredient not found in your profile");
        }
        profileRepository.save(profile);
        audit(profile, AuditAction.PREFERENCE_UPDATED, "Avoided ingredient removed");
    }

    @Override
    @Transactional(readOnly = true)
    public String buildSnapshot(Long userId) {
        Optional<CustomerFoodProfile> found = profileRepository.findDetailedByUserId(userId);
        if (found.isEmpty()) {
            return "No food profile";
        }
        CustomerFoodProfile p = found.get();
        String allergies = p.getAllergies().stream()
                .map(a -> a.getAllergen().getName() + " (" + a.getSeverity() + ")").sorted()
                .collect(Collectors.joining(", "));
        String diets = p.getDietaryPreferences().stream()
                .map(d -> d.getDietType() + (d.isStrict() ? " (strict)" : "")).sorted()
                .collect(Collectors.joining(", "));
        String avoided = p.getAvoidedIngredients().stream().map(Ingredient::getName).sorted()
                .collect(Collectors.joining(", "));
        String text = "Allergies: " + (allergies.isEmpty() ? "none" : allergies)
                + "; Diets: " + (diets.isEmpty() ? "none" : diets)
                + "; Avoids: " + (avoided.isEmpty() ? "none" : avoided)
                + "; Max spice: " + p.getMaxSpiceLevel()
                + "; Profile confirmed complete: " + p.isConfirmedComplete();
        return TextUtil.truncate(text, 2000);
    }

    private CustomerFoodProfile loadOrCreate(Long userId) {
        return profileRepository.findDetailedByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            CustomerFoodProfile created = new CustomerFoodProfile();
            created.setUser(user);
            return profileRepository.save(created);
        });
    }

    private void audit(CustomerFoodProfile profile, AuditAction action, String details) {
        auditService.log(profile.getUser().getEmail(), action, "CustomerFoodProfile", profile.getId(), details);
    }
}
