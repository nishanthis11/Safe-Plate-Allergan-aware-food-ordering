package com.example.smartfoodordering.controller;

import com.example.smartfoodordering.dto.request.RegisterRequest;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String form(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerCustomer(form);
        } catch (BusinessRuleException e) {
            result.reject("register.failed", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
