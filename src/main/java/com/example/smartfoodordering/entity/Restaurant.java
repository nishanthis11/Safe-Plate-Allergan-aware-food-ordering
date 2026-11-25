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
@Table(name = "restaurants")
@Getter @Setter @NoArgsConstructor
public class Restaurant extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 600)
    private String description;

    @Column(nullable = false, length = 60)
    private String cuisine;

    @Column(length = 250)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = new BigDecimal("30.00");

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuItem> menuItems = new ArrayList<>();
}
