package com.cinema.reservation.repository;

import com.cinema.reservation.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    // Single Responsibility - tylko operacje na Cinema
    List<Cinema> findByCity(String city);

    List<Cinema> findByNameContainingIgnoreCase(String name);

    // Business queries
    @Query("SELECT c FROM Cinema c JOIN FETCH c.halls WHERE c.city = :city")
    List<Cinema> findCinemasWithHallsByCity(@Param("city") String city);

    // Performance optimization - tylko podstawowe info
    @Query("SELECT c.id, c.name, c.city FROM Cinema c ORDER BY c.name")
    List<Object[]> findBasicCinemaInfo();
}