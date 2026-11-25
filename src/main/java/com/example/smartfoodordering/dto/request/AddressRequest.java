package com.example.smartfoodordering.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    @NotBlank @Size(max = 40)
    private String label;

    @NotBlank @Size(max = 200)
    private String line1;

    @Size(max = 200)
    private String line2;

    @NotBlank @Size(max = 80)
    private String city;

    @NotBlank @Size(max = 80)
    private String state;

    @NotBlank @Pattern(regexp = "^[A-Za-z0-9 -]{3,12}$", message = "Enter a valid postal code")
    private String postalCode;

    @NotBlank @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number")
    private String contactPhone;

    private boolean defaultAddress;
}
