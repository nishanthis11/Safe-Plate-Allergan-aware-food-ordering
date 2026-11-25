package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank @Size(max = 100)
    private String fullName;

    @NotBlank @Email @Size(max = 150)
    private String email;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @StrongPassword
    private String password;

    @NotBlank
    private String confirmPassword;
}
