package com.cinema.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor  // publiczny konstruktor ze wszystkimi polami
@NoArgsConstructor   // publiczny konstruktor bezargumentowy (np. dla Jacksona)
public class UserCreateRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserType userType;

    // Regular user specific
    private String phoneNumber;

    // Admin user specific
    private String department;
    private String adminLevel;
}
