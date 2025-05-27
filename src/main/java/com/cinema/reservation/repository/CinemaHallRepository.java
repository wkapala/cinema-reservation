package com.cinema.reservation.repository;

import com.cinema.reservation.entity.CinemaHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaHallRepository extends JpaRepository<CinemaHall, Long> {

    // Single Responsibility - tylko operacje na CinemaHall
    List<CinemaHall> findByCinemaId(Long cinemaId);

    List<CinemaHall> findByHallType(CinemaHall.HallType hallType);

    // Business logic - sale o określonej pojemności
    @Query("SELECT h FROM CinemaHall h WHERE h.totalSeats >= :minSeats ORDER BY h.totalSeats")
    List<CinemaHall> findHallsWithMinimumCapacity(@Param("minSeats") Integer minSeats);

    // Dependency Inversion - repository nie wie jak używa tego service
    @Query("SELECT h FROM CinemaHall h WHERE h.cinema.id = :cinemaId AND h.hallType = :hallType")
    List<CinemaHall> findByCinemaIdAndHallType(@Param("cinemaId") Long cinemaId,
                                               @Param("hallType") CinemaHall.HallType hallType);
}