package com.cinema.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserved_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"screening_id", "row_number", "seat_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    // Helper method to display seat (e.g., "A5")
    public String getSeatDisplay() {
        char rowLetter = (char) ('A' + rowNumber - 1);
        return rowLetter + String.valueOf(seatNumber);
    }
}