package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.AllergySeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllergyRequest {
    @NotNull
    private Long allergenId;

    @NotNull
    private AllergySeverity severity = AllergySeverity.MODERATE;

    @Size(max = 300)
    private String notes;
}
