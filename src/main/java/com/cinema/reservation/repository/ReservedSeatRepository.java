package com.cinema.reservation.repository;

import com.cinema.reservation.entity.ReservedSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {

    // Single Responsibility - tylko operacje na ReservedSeat
    List<ReservedSeat> findByScreeningId(Long screeningId);

    List<ReservedSeat> findByReservationId(Long reservationId);

    // Business logic - sprawdzenie czy miejsce jest zajęte
    boolean existsByScreeningIdAndRowNumberAndSeatNumber(Long screeningId, Integer rowNumber, Integer seatNumber);

    // Interface Segregation - każdy interface ma konkretny cel
    @Query("SELECT rs FROM ReservedSeat rs WHERE rs.screening.id = :screeningId AND rs.rowNumber = :rowNumber")
    List<ReservedSeat> findByScreeningIdAndRow(@Param("screeningId") Long screeningId,
                                               @Param("rowNumber") Integer rowNumber);

    // Dependency Inversion - service decyduje jak wykorzystać te dane
    @Query("SELECT CONCAT(rs.rowNumber, '-', rs.seatNumber) FROM ReservedSeat rs WHERE rs.screening.id = :screeningId")
    List<String> findOccupiedSeatsForScreening(@Param("screeningId") Long screeningId);

    // Performance optimization - count bez pobierania obiektów
    @Query("SELECT COUNT(rs) FROM ReservedSeat rs WHERE rs.screening.id = :screeningId")
    long countReservedSeatsForScreening(@Param("screeningId") Long screeningId);
}