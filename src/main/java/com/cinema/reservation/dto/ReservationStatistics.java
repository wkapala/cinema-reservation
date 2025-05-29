package com.cinema.reservation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReservationStatistics {
    private Long confirmedTodayCount;
    private BigDecimal monthlyRevenue;
    private Long weeklyReservationsCount;
}