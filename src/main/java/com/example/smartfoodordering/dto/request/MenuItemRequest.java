package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.SpiceLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuItemRequest {
    @NotBlank @Size(max = 120)
    private String name;

    @Size(max = 600)
    private String description;

    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotBlank @Size(max = 60)
    private String cuisine;

    @NotNull
    private SpiceLevel spiceLevel = SpiceLevel.NONE;

    private boolean available = true;
    private boolean allergenInfoComplete;

    private List<DietType> dietTags = new ArrayList<>();
    private List<Long> ingredientIds = new ArrayList<>();
    private List<Long> declaredAllergenIds = new ArrayList<>();
    private List<Long> mayContainAllergenIds = new ArrayList<>();
}
