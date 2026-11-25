package com.example.smartfoodordering.dto.request;

import com.example.smartfoodordering.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    @NotNull
    private OrderStatus status;

    @Size(max = 250)
    private String note;
}
