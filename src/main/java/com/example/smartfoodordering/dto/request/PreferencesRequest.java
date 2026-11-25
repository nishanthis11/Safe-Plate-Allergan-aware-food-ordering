package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.SpiceLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreferencesRequest {
    @NotNull
    private SpiceLevel maxSpiceLevel = SpiceLevel.MEDIUM;

    /** Comma separated list, e.g. "Thai, South Indian". */
    @Size(max = 300)
    private String favoriteCuisines;

    private boolean confirmedComplete;
}
