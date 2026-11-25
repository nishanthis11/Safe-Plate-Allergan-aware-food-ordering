package com.example.smartfoodordering.entity;

import com.example.smartfoodordering.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "delivery_addresses")
@Getter @Setter @NoArgsConstructor
public class DeliveryAddress extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(nullable = false, length = 200)
    private String line1;

    @Column(length = 200)
    private String line2;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(nullable = false, length = 12)
    private String postalCode;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Column(nullable = false)
    private boolean defaultAddress = false;

    public String asSingleLine() {
        return java.util.stream.Stream.of(line1, line2, city, state, postalCode)
                .filter(part -> part != null && !part.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
