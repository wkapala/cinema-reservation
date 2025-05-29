package com.cinema.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor   // generuje publiczny konstruktor (userId, screeningId, seats)
@NoArgsConstructor    // potrzebny np. dla Jacksona
public class ReservationCreateRequest {
    private Long userId;
    private Long screeningId;
    private List<SeatRequest> seats;
}
