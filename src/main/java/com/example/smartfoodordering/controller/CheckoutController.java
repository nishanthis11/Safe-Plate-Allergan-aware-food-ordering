package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.CheckoutRequest;
import com.example.smartfoodordering.dto.response.CartResponse;
import com.example.smartfoodordering.dto.response.OrderDetailResponse;
import com.example.smartfoodordering.enums.PaymentMethod;
import com.example.smartfoodordering.exception.AllergenAcknowledgementRequiredException;
import com.example.smartfoodordering.exception.AllergenConflictException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.AddressService;
import com.example.smartfoodordering.service.CartService;
import com.example.smartfoodordering.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class CheckoutController {
    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;

    public CheckoutController(CartService cartService, AddressService addressService, OrderService orderService) {
        this.cartService = cartService;
        this.addressService = addressService;
        this.orderService = orderService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal CustomUserDetails user, Model model, RedirectAttributes ra) {
        CartResponse cart = cartService.getCart(user.getId());
        if (cart.isEmpty()) {
            ra.addFlashAttribute("error", "Your cart is empty");
            return "redirect:/cart";
        }
        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addressService.list(user.getId()));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("form", new CheckoutRequest());
        return "checkout";
    }

    @PostMapping
    public String place(@AuthenticationPrincipal CustomUserDetails user,
                        @Valid @ModelAttribute("form") CheckoutRequest form, BindingResult result,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", FlashSupport.errors(result));
            return "redirect:/checkout";
        }
        try {
            OrderDetailResponse order = orderService.placeOrder(user.getId(), user.getUsername(), form);
            ra.addFlashAttribute("success", "Order " + order.orderNumber() + " placed");
            return "redirect:/orders/" + order.id();
        } catch (AllergenConflictException e) {
            ra.addFlashAttribute("error", "Order blocked for your safety: " + e.getResult().summary());
        } catch (AllergenAcknowledgementRequiredException e) {
            ra.addFlashAttribute("error", "Please read and acknowledge the warnings before placing this order");
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/checkout";
    }
}
