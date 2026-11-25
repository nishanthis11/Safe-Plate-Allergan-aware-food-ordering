package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.AddressRequest;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.AddressService;
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
@RequestMapping("/customer/addresses")
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("addresses", addressService.list(user.getId()));
        model.addAttribute("form", new AddressRequest());
        return "addresses";
    }

    @PostMapping
    public String add(@AuthenticationPrincipal CustomUserDetails user, @Valid @ModelAttribute("form") AddressRequest form,
                      BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("addresses", addressService.list(user.getId()));
            return "addresses";
        }
        addressService.add(user.getId(), form);
        ra.addFlashAttribute("success", "Address saved");
        return "redirect:/customer/addresses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id, RedirectAttributes ra) {
        try {
            addressService.delete(user.getId(), id);
            ra.addFlashAttribute("success", "Address removed");
        } catch (ResourceNotFoundException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/addresses";
    }
}
