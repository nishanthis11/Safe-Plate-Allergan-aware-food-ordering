package com.example.smartfoodordering.controller;

import java.util.stream.Collectors;
import org.springframework.validation.BindingResult;

final class FlashSupport {
    private FlashSupport() {}

    static String errors(BindingResult result) {
        String message = result.getAllErrors().stream()
                .map(e -> e.getDefaultMessage() == null ? "Invalid input" : e.getDefaultMessage())
                .distinct().collect(Collectors.joining("; "));
        return message.isBlank() ? "Please check your input" : message;
    }
}
