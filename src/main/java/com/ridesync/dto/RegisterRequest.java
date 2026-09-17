package com.ridesync.dto;

import com.ridesync.model.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String phone;
    private Role role; // ROLE_RIDER or ROLE_DRIVER
    // If Driver
    private String licenseNumber;
}
