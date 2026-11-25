package com.example.smartfoodordering.security;

import com.example.smartfoodordering.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final Long restaurantId;
    private final boolean enabled;
    private final boolean locked;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.restaurantId = user.getRestaurant() == null ? null : user.getRestaurant().getId();
        this.enabled = user.isEnabled();
        this.locked = user.isLocked();
        this.authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName().name()))
                .collect(Collectors.toSet());
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public Long getRestaurantId() { return restaurantId; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !locked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }

    public boolean hasAuthority(String authority) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    public boolean isAdmin() { return hasAuthority("ROLE_ADMIN"); }
    public boolean isManager() { return hasAuthority("ROLE_RESTAURANT_MANAGER"); }
    public boolean isKitchen() { return hasAuthority("ROLE_KITCHEN_STAFF"); }
}
