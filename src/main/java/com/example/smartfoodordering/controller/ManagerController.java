package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.MenuItemRequest;
import com.example.smartfoodordering.dto.request.StatusUpdateRequest;
import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.SpiceLevel;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.CatalogService;
import com.example.smartfoodordering.service.MenuService;
import com.example.smartfoodordering.service.OrderService;
import com.example.smartfoodordering.service.ReportService;
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
@RequestMapping("/manager")
@PreAuthorize("hasAuthority('ROLE_RESTAURANT_MANAGER')")
public class ManagerController {
    private final MenuService menuService;
    private final OrderService orderService;
    private final ReportService reportService;
    private final CatalogService catalogService;

    public ManagerController(MenuService menuService, OrderService orderService, ReportService reportService,
                             CatalogService catalogService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.reportService = reportService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("stats", reportService.statsFor(user.getRestaurantId()));
        model.addAttribute("orders", orderService.listForStaff(user).stream().limit(8).toList());
        return "manager/dashboard";
    }

    // ------------------------------------------------------------------ menu

    @GetMapping("/menu")
    public String menu(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("items", menuService.getMenuForManager(user));
        return "manager/menu";
    }

    @GetMapping("/menu/new")
    public String newItem(Model model) {
        model.addAttribute("menuForm", new MenuItemRequest());
        model.addAttribute("itemId", null);
        addFormLists(model);
        return "manager/menu-form";
    }

    @GetMapping("/menu/{id}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, Model model) {
        model.addAttribute("menuForm", menuService.getFormForEdit(user, id));
        model.addAttribute("itemId", id);
        addFormLists(model);
        return "manager/menu-form";
    }

    @PostMapping("/menu")
    public String create(@AuthenticationPrincipal CustomUserDetails user,
                         @Valid @ModelAttribute("menuForm") MenuItemRequest form, BindingResult result, Model model,
                         RedirectAttributes ra) {
        return save(user, null, form, result, model, ra);
    }

    @PostMapping("/menu/{id}")
    public String update(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                         @Valid @ModelAttribute("menuForm") MenuItemRequest form, BindingResult result, Model model,
                         RedirectAttributes ra) {
        return save(user, id, form, result, model, ra);
    }

    @PostMapping("/menu/{id}/availability")
    public String availability(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                               @RequestParam boolean available, RedirectAttributes ra) {
        try {
            menuService.setAvailability(user, id, available);
            ra.addFlashAttribute("success", available ? "Item is now available" : "Item hidden from customers");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/menu";
    }

    // ---------------------------------------------------------------- orders

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("orders", orderService.listForStaff(user));
        model.addAttribute("basePath", "/manager");
        return "staff/orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getForStaff(user, id));
        model.addAttribute("basePath", "/manager");
        model.addAttribute("statusForm", new StatusUpdateRequest());
        return "staff/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                               @Valid @ModelAttribute StatusUpdateRequest form, BindingResult result,
                               RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", FlashSupport.errors(result));
        } else {
            try {
                orderService.updateStatus(user, id, form.getStatus(), form.getNote());
                ra.addFlashAttribute("success", "Order updated");
            } catch (BusinessRuleException e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/manager/orders/" + id;
    }

    // --------------------------------------------------------------- helpers

    private String save(CustomUserDetails user, Long id, MenuItemRequest form, BindingResult result, Model model,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("itemId", id);
            addFormLists(model);
            return "manager/menu-form";
        }
        try {
            menuService.save(user, id, form);
            ra.addFlashAttribute("success", "Menu item saved");
            return "redirect:/manager/menu";
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            model.addAttribute("itemId", id);
            model.addAttribute("error", e.getMessage());
            addFormLists(model);
            return "manager/menu-form";
        }
    }

    private void addFormLists(Model model) {
        model.addAttribute("allergens", catalogService.allergens());
        model.addAttribute("ingredients", catalogService.ingredients());
        model.addAttribute("dietTypes", DietType.values());
        model.addAttribute("spiceLevels", SpiceLevel.values());
    }
}
