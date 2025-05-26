package com.cinema.reservation.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@DiscriminatorValue("ADMIN")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminUser extends User {

    private String department;
    private String adminLevel; // SUPER_ADMIN, CINEMA_MANAGER, etc.

    @Override
    @Transient
    public List<String> getRoles() {
        return List.of("ROLE_ADMIN", "ROLE_USER");
    }

    @Override
    public boolean canMakeReservation() {
        return true;
    }

    @Override
    public boolean canManageMovies() {
        return true;
    }

    @Override
    public boolean canViewAllReservations() {
        return true;
    }
}