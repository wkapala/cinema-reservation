package com.cinema.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor  // publiczny konstruktor ze wszystkimi polami
@NoArgsConstructor   // publiczny konstruktor bezargumentowy (np. dla Jacksona)
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber; // for RegularUser
    private String department;  // for AdminUser
}
