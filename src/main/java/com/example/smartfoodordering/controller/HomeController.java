package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.MenuService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final MenuService menuService;

    public HomeController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("restaurants", menuService.listActiveRestaurants());
        return "home";
    }

    /** Sends each role to its own landing page after login. */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user) {
        if (user.isAdmin()) return "redirect:/admin";
        if (user.isManager()) return "redirect:/manager";
        if (user.isKitchen()) return "redirect:/kitchen";
        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
