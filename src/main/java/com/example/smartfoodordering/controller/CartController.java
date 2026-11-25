package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.CartAddRequest;
import com.example.smartfoodordering.exception.AllergenConflictException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.CartService;
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
@RequestMapping("/cart")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("cart", cartService.getCart(user.getId()));
        return "cart";
    }

    @PostMapping("/add")
    public String add(@AuthenticationPrincipal CustomUserDetails user, @Valid @ModelAttribute CartAddRequest form,
                      BindingResult result, RedirectAttributes ra) {
        String back = form.getRestaurantId() != null ? "redirect:/restaurants/" + form.getRestaurantId() : "redirect:/cart";
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", FlashSupport.errors(result));
            return back;
        }
        try {
            cartService.addItem(user.getId(), form);
            ra.addFlashAttribute("success", "Added to your cart");
        } catch (AllergenConflictException e) {
            ra.addFlashAttribute("error", "Blocked for your safety: " + e.getResult().summary());
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back;
    }

    @PostMapping("/items/{id}/quantity")
    public String quantity(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id,
                           @RequestParam int quantity, RedirectAttributes ra) {
        try {
            cartService.updateQuantity(user.getId(), id, quantity);
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/items/{id}/remove")
    public String remove(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, RedirectAttributes ra) {
        try {
            cartService.removeItem(user.getId(), id);
        } catch (ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(@AuthenticationPrincipal CustomUserDetails user) {
        cartService.clear(user.getId());
        return "redirect:/cart";
    }
}
