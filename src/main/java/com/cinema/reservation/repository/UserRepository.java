package com.cinema.reservation.repository;

import com.cinema.reservation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Single Responsibility - tylko operacje na User
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Custom query dla business logic
    @Query("SELECT u FROM User u WHERE u.createdAt >= :fromDate")
    List<User> findRecentUsers(@Param("fromDate") LocalDateTime fromDate);

    // Repository pattern - enkapsulacja dostępu do danych
    @Query("SELECT COUNT(u) FROM User u WHERE u.class = 'REGULAR'")
    long countRegularUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.class = 'ADMIN'")
    long countAdminUsers();
}