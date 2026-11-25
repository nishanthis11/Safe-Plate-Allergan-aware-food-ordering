package com.example.smartfoodordering.security;

import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.service.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventsListener {
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    public AuthenticationEventsListener(LoginAttemptService loginAttemptService, AuditService auditService) {
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        loginAttemptService.recordSuccess(email);
        auditService.log(email, AuditAction.LOGIN_SUCCESS, "User", null, "Login succeeded");
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = String.valueOf(event.getAuthentication().getName());
        loginAttemptService.recordFailure(email);
        auditService.log(email, AuditAction.LOGIN_FAILURE, "User", null, "Bad credentials");
    }
}
