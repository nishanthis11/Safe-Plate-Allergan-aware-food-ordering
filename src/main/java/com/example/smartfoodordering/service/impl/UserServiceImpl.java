package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.request.RegisterRequest;
import com.example.smartfoodordering.dto.request.StaffRequest;
import com.example.smartfoodordering.dto.response.UserResponse;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.Role;
import com.example.smartfoodordering.entity.User;
import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.enums.RoleName;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.repository.CustomerFoodProfileRepository;
import com.example.smartfoodordering.repository.RestaurantRepository;
import com.example.smartfoodordering.repository.RoleRepository;
import com.example.smartfoodordering.repository.UserRepository;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.service.UserService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RestaurantRepository restaurantRepository;
    private final CustomerFoodProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           RestaurantRepository restaurantRepository, CustomerFoodProfileRepository profileRepository,
                           PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.restaurantRepository = restaurantRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public void registerCustomer(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.getRoles().add(findRole(RoleName.ROLE_CUSTOMER));
        User saved = userRepository.save(user);

        CustomerFoodProfile profile = new CustomerFoodProfile();
        profile.setUser(saved);
        profileRepository.save(profile);

        auditService.log(email, AuditAction.REGISTER, "User", saved.getId(), "Customer registered");
    }

    @Override
    public void createStaff(StaffRequest request, String actorEmail) {
        if (request.getRole() != RoleName.ROLE_RESTAURANT_MANAGER && request.getRole() != RoleName.ROLE_KITCHEN_STAFF) {
            throw new BusinessRuleException("Only restaurant manager or kitchen staff accounts can be created here");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.getRoles().add(findRole(request.getRole()));
        user.setRestaurant(restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found")));
        User saved = userRepository.save(user);
        auditService.log(actorEmail, AuditAction.REGISTER, "User", saved.getId(), "Staff account created: " + request.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getEmail))
                .map(u -> new UserResponse(u.getId(), u.getFullName(), u.getEmail(),
                        u.getRoles().stream().map(r -> r.getName().name().replace("ROLE_", "")).sorted()
                                .collect(Collectors.joining(", ")),
                        u.getRestaurant() == null ? null : u.getRestaurant().getName(), u.isEnabled()))
                .toList();
    }

    @Override
    public void setEnabled(Long userId, boolean enabled, String actorEmail) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new BusinessRuleException("You cannot change your own account status");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        auditService.log(actorEmail, AuditAction.PROFILE_UPDATED, "User", userId, enabled ? "Account enabled" : "Account disabled");
    }

    private Role findRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + name));
    }
}
