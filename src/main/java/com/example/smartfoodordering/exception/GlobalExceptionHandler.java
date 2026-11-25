package com.example.smartfoodordering.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Turns domain exceptions into friendly pages. Spring Security's own AccessDeniedException is deliberately not
 * handled here so the security filter chain can redirect to the access-denied page.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String notFound(ResourceNotFoundException ex, Model model, HttpServletResponse response) {
        return render(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), model, response);
    }

    @ExceptionHandler(AccessDeniedAppException.class)
    public String forbidden(AccessDeniedAppException ex, Model model, HttpServletResponse response) {
        return render(HttpStatus.FORBIDDEN, "Access denied", ex.getMessage(), model, response);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public String businessRule(BusinessRuleException ex, Model model, HttpServletResponse response) {
        return render(HttpStatus.BAD_REQUEST, "Request could not be completed", ex.getMessage(), model, response);
    }

    private String render(HttpStatus status, String title, String message, Model model, HttpServletResponse response) {
        response.setStatus(status.value());
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "error";
    }
}
