package com.cinema.reservation.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@DiscriminatorValue("REGULAR")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RegularUser extends User {

    private String phoneNumber;

    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations;

    @Override
    public List<String> getRoles() {
        return List.of("ROLE_USER");
    }

    @Override
    public boolean canMakeReservation() {
        return true;
    }

    @Override
    public boolean canManageMovies() {
        return false;
    }

    @Override
    public boolean canViewAllReservations() {
        return false;
    }
}