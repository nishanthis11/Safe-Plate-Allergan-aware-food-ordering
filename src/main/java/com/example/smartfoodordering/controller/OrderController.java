package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("orders", orderService.listForCustomer(user.getId()));
        return "orders";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getForCustomer(user.getId(), id));
        return "order-detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                         @RequestParam(required = false) String reason, RedirectAttributes ra) {
        try {
            orderService.cancelByCustomer(user.getId(), user.getUsername(), id, reason);
            ra.addFlashAttribute("success", "Order cancelled");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
