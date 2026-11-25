package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.RegisterRequest;
import com.example.smartfoodordering.dto.request.StaffRequest;
import com.example.smartfoodordering.dto.response.UserResponse;
import java.util.List;

public interface UserService {
    void registerCustomer(RegisterRequest request);

    void createStaff(StaffRequest request, String actorEmail);

    List<UserResponse> listUsers();

    void setEnabled(Long userId, boolean enabled, String actorEmail);
}
