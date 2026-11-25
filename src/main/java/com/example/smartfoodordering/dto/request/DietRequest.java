package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.DietType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DietRequest {
    @NotNull
    private DietType dietType;
    private boolean strict = true;
}
