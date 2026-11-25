package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.MenuService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Public browsing. Safety warnings appear only for a signed-in customer. */
@Controller
public class RestaurantController {
    private final MenuService menuService;

    public RestaurantController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/restaurants")
    public String list(Model model) {
        model.addAttribute("restaurants", menuService.listActiveRestaurants());
        return "home";
    }

    @GetMapping("/restaurants/{id}")
    public String menu(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
        Long customerId = user != null && user.hasAuthority("ROLE_CUSTOMER") ? user.getId() : null;
        model.addAttribute("restaurant", menuService.getActiveRestaurant(id));
        model.addAttribute("menu", menuService.getMenuForCustomer(id, customerId));
        model.addAttribute("isCustomer", customerId != null);
        return "restaurant-menu";
    }
}
