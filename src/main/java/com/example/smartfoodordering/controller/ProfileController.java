package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.AllergyRequest;
import com.example.smartfoodordering.dto.request.AvoidIngredientRequest;
import com.example.smartfoodordering.dto.request.DietRequest;
import com.example.smartfoodordering.dto.request.PreferencesRequest;
import com.example.smartfoodordering.dto.response.FoodProfileResponse;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.SpiceLevel;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.CatalogService;
import com.example.smartfoodordering.service.FoodProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class ProfileController {
    private final FoodProfileService profileService;
    private final CatalogService catalogService;

    public ProfileController(FoodProfileService profileService, CatalogService catalogService) {
        this.profileService = profileService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        FoodProfileResponse profile = profileService.getProfile(user.getId());
        PreferencesRequest prefs = new PreferencesRequest();
        prefs.setMaxSpiceLevel(profile.maxSpiceLevel());
        prefs.setFavoriteCuisines(String.join(", ", profile.favoriteCuisines()));
        prefs.setConfirmedComplete(profile.confirmedComplete());
        model.addAttribute("profile", profile);
        model.addAttribute("prefs", prefs);
        model.addAttribute("allergyForm", new AllergyRequest());
        model.addAttribute("dietForm", new DietRequest());
        model.addAttribute("avoidForm", new AvoidIngredientRequest());
        model.addAttribute("allergens", catalogService.allergens());
        model.addAttribute("ingredients", catalogService.ingredients());
        model.addAttribute("severities", AllergySeverity.values());
        model.addAttribute("dietTypes", DietType.values());
        model.addAttribute("spiceLevels", SpiceLevel.values());
        return "profile";
    }

    @PostMapping("/preferences")
    public String preferences(@AuthenticationPrincipal CustomUserDetails user,
                              @Valid @ModelAttribute PreferencesRequest form, BindingResult result,
                              RedirectAttributes ra) {
        return run(result, ra, "Preferences saved", () -> profileService.updatePreferences(user.getId(), form));
    }

    @PostMapping("/allergies")
    public String addAllergy(@AuthenticationPrincipal CustomUserDetails user,
                             @Valid @ModelAttribute AllergyRequest form, BindingResult result, RedirectAttributes ra) {
        return run(result, ra, "Allergy saved", () -> profileService.addAllergy(user.getId(), form));
    }

    @PostMapping("/allergies/{id}/delete")
    public String removeAllergy(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                                RedirectAttributes ra) {
        return run(null, ra, "Allergy removed", () -> profileService.removeAllergy(user.getId(), id));
    }

    @PostMapping("/diets")
    public String addDiet(@AuthenticationPrincipal CustomUserDetails user,
                          @Valid @ModelAttribute DietRequest form, BindingResult result, RedirectAttributes ra) {
        return run(result, ra, "Dietary preference saved", () -> profileService.addDiet(user.getId(), form));
    }

    @PostMapping("/diets/{id}/delete")
    public String removeDiet(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                             RedirectAttributes ra) {
        return run(null, ra, "Dietary preference removed", () -> profileService.removeDiet(user.getId(), id));
    }

    @PostMapping("/avoided")
    public String addAvoided(@AuthenticationPrincipal CustomUserDetails user,
                             @Valid @ModelAttribute AvoidIngredientRequest form, BindingResult result,
                             RedirectAttributes ra) {
        return run(result, ra, "Ingredient added to your avoid list",
                () -> profileService.addAvoidedIngredient(user.getId(), form.getIngredientId()));
    }

    @PostMapping("/avoided/{id}/delete")
    public String removeAvoided(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                                RedirectAttributes ra) {
        return run(null, ra, "Ingredient removed from your avoid list",
                () -> profileService.removeAvoidedIngredient(user.getId(), id));
    }

    private String run(BindingResult result, RedirectAttributes ra, String success, Runnable action) {
        if (result != null && result.hasErrors()) {
            ra.addFlashAttribute("error", FlashSupport.errors(result));
            return "redirect:/profile";
        }
        try {
            action.run();
            ra.addFlashAttribute("success", success);
        } catch (ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
