package com.cinema.reservation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatistics {
    private Long regularUsersCount;
    private Long adminUsersCount;
    private Long recentUsersCount;
    private Long totalUsers;
}