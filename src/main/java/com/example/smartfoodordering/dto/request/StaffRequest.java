package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.RoleName;
import com.example.smartfoodordering.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffRequest {
    @NotBlank @Size(max = 100)
    private String fullName;

    @NotBlank @Email @Size(max = 150)
    private String email;

    @StrongPassword
    private String password;

    @NotNull
    private RoleName role;

    @NotNull
    private Long restaurantId;
}
