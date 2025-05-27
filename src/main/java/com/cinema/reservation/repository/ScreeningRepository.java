package com.cinema.reservation.repository;

import com.cinema.reservation.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    // Single Responsibility - tylko operacje na Screening
    List<Screening> findByMovieId(Long movieId);

    List<Screening> findByHallId(Long hallId);

    // Business logic - dostępne seanse
    @Query("SELECT s FROM Screening s WHERE s.startTime >= :fromTime AND s.availableSeats > 0 ORDER BY s.startTime")
    List<Screening> findAvailableScreenings(@Param("fromTime") LocalDateTime fromTime);

    // Konkretny dzień
    @Query("SELECT s FROM Screening s WHERE DATE(s.startTime) = DATE(:date) ORDER BY s.startTime")
    List<Screening> findByDate(@Param("date") LocalDateTime date);

    // Seanse dla konkretnego filmu w przyszłości
    @Query("SELECT s FROM Screening s WHERE s.movie.id = :movieId AND s.startTime >= CURRENT_TIMESTAMP ORDER BY s.startTime")
    List<Screening> findUpcomingScreeningsForMovie(@Param("movieId") Long movieId);

    // Seanse w konkretnej sali w określonym czasie (collision detection)
    @Query("SELECT s FROM Screening s WHERE s.hall.id = :hallId " +
            "AND ((s.startTime BETWEEN :startTime AND :endTime) " +
            "OR (s.endTime BETWEEN :startTime AND :endTime) " +
            "OR (:startTime BETWEEN s.startTime AND s.endTime))")
    List<Screening> findConflictingScreenings(@Param("hallId") Long hallId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    // Dependency Inversion - repository nie zależy od konkretnej implementacji
    @Query("SELECT s FROM Screening s WHERE s.availableSeats >= :requiredSeats AND s.startTime >= CURRENT_TIMESTAMP")
    List<Screening> findScreeningsWithAvailableSeats(@Param("requiredSeats") Integer requiredSeats);

    // Performance query - tylko podstawowe dane
    @Query("SELECT s.id, s.startTime, s.availableSeats, s.price FROM Screening s WHERE s.movie.id = :movieId")
    List<Object[]> findBasicScreeningInfoByMovie(@Param("movieId") Long movieId);
}