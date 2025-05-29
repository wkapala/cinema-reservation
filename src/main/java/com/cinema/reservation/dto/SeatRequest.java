package com.cinema.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor   // generuje publiczny konstruktor (rowNumber, seatNumber)
@NoArgsConstructor    // potrzebny np. dla Jacksona
public class SeatRequest {
    private Integer rowNumber;
    private Integer seatNumber;
}
