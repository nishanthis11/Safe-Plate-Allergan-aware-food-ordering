package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.RestaurantRequest;
import com.example.smartfoodordering.dto.request.StaffRequest;
import com.example.smartfoodordering.enums.RoleName;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.service.MenuService;
import com.example.smartfoodordering.service.ReportService;
import com.example.smartfoodordering.service.UserService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {
    private final ReportService reportService;
    private final UserService userService;
    private final MenuService menuService;
    private final AuditService auditService;

    public AdminController(ReportService reportService, UserService userService, MenuService menuService,
                           AuditService auditService) {
        this.reportService = reportService;
        this.userService = userService;
        this.menuService = menuService;
        this.auditService = auditService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", reportService.statsFor(null));
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("restaurants", menuService.listAllRestaurants());
        model.addAttribute("staffForm", new StaffRequest());
        model.addAttribute("roles", new RoleName[]{RoleName.ROLE_RESTAURANT_MANAGER, RoleName.ROLE_KITCHEN_STAFF});
        return "admin/users";
    }

    @PostMapping("/users")
    public String createStaff(@AuthenticationPrincipal CustomUserDetails admin,
                              @Valid @ModelAttribute("staffForm") StaffRequest form, BindingResult result, Model model,
                              RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                userService.createStaff(form, admin.getUsername());
                ra.addFlashAttribute("success", "Staff account created");
                return "redirect:/admin/users";
            } catch (BusinessRuleException | ResourceNotFoundException e) {
                result.reject("staff.failed", e.getMessage());
            }
        }
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("restaurants", menuService.listAllRestaurants());
        model.addAttribute("roles", new RoleName[]{RoleName.ROLE_RESTAURANT_MANAGER, RoleName.ROLE_KITCHEN_STAFF});
        return "admin/users";
    }

    @PostMapping("/users/{id}/enabled")
    public String setEnabled(@AuthenticationPrincipal CustomUserDetails admin, @PathVariable Long id,
                             @RequestParam boolean enabled, RedirectAttributes ra) {
        try {
            userService.setEnabled(id, enabled, admin.getUsername());
            ra.addFlashAttribute("success", "Account updated");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/restaurants")
    public String restaurants(Model model) {
        model.addAttribute("restaurants", menuService.listAllRestaurants());
        model.addAttribute("form", new RestaurantRequest());
        return "admin/restaurants";
    }

    @PostMapping("/restaurants")
    public String createRestaurant(@AuthenticationPrincipal CustomUserDetails admin,
                                   @Valid @ModelAttribute("form") RestaurantRequest form, BindingResult result,
                                   Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("restaurants", menuService.listAllRestaurants());
            return "admin/restaurants";
        }
        menuService.createRestaurant(form, admin.getUsername());
        ra.addFlashAttribute("success", "Restaurant created");
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/restaurants/{id}/active")
    public String setActive(@AuthenticationPrincipal CustomUserDetails admin, @PathVariable Long id,
                            @RequestParam boolean active, RedirectAttributes ra) {
        try {
            menuService.setRestaurantActive(id, active, admin.getUsername());
            ra.addFlashAttribute("success", "Restaurant updated");
        } catch (ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/restaurants";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("entries", auditService.recent());
        return "admin/audit";
    }
}
