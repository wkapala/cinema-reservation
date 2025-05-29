package com.cinema.reservation.repository;

import com.cinema.reservation.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Single Responsibility - tylko operacje na Reservation
    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByScreeningId(Long screeningId);

    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    // Business logic queries
    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    // User-specific reservations with pagination
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    // Admin queries - może widzieć wszystkie
    @Query("SELECT r FROM Reservation r WHERE r.createdAt >= :fromDate ORDER BY r.createdAt DESC")
    List<Reservation> findRecentReservations(@Param("fromDate") LocalDateTime fromDate);

    // Business analytics
    // Przełączone na zapytanie natywne, żeby uniknąć błędów porównywania TIMESTAMP z DATE
    @Query(
            value = "SELECT COUNT(*) FROM reservations r " +
                    "WHERE r.status = 'CONFIRMED' " +
                    "  AND CAST(r.created_at AS date) = CURRENT_DATE",
            nativeQuery = true
    )
    long countTodayConfirmedReservations();

    @Query("SELECT SUM(r.totalPrice) FROM Reservation r WHERE r.status = 'CONFIRMED' AND r.createdAt >= :fromDate")
    Double calculateRevenue(@Param("fromDate") LocalDateTime fromDate);

    // Expired reservations cleanup
    @Query("SELECT r FROM Reservation r WHERE r.status = 'PENDING' AND r.createdAt < :expiredBefore")
    List<Reservation> findExpiredPendingReservations(@Param("expiredBefore") LocalDateTime expiredBefore);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'EXPIRED' WHERE r.status = 'PENDING' AND r.createdAt < :expiredBefore")
    int expirePendingReservations(@Param("expiredBefore") LocalDateTime expiredBefore);

    // User reservation history with screening details
    @Query("SELECT r FROM Reservation r JOIN FETCH r.screening s JOIN FETCH s.movie m WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Reservation> findUserReservationsWithDetails(@Param("userId") Long userId);

    // Conflict detection
    boolean existsByUserIdAndScreeningId(Long userId, Long screeningId);
}
