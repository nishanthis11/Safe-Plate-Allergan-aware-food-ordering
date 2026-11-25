package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.RecommendationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer/recommendations")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("recommendations", recommendationService.recommend(user.getId()));
        return "recommendations";
    }
}
