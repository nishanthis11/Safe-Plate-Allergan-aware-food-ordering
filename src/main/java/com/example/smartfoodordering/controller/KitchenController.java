package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.StatusUpdateRequest;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.OrderService;
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

/** Kitchen staff see the order queue, allergen warnings and items; never the customer's full food profile. */
@Controller
@RequestMapping("/kitchen")
@PreAuthorize("hasAnyAuthority('ROLE_KITCHEN_STAFF', 'ROLE_RESTAURANT_MANAGER')")
public class KitchenController {
    private final OrderService orderService;

    public KitchenController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String queue(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("orders", orderService.kitchenQueue(user));
        model.addAttribute("basePath", "/kitchen");
        return "kitchen/queue";
    }

    @GetMapping("/orders/{id}")
    public String order(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getForStaff(user, id));
        model.addAttribute("basePath", "/kitchen");
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
        return "redirect:/kitchen/orders/" + id;
    }
}
