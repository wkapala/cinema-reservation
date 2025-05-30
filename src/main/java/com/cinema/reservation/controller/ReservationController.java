package com.cinema.reservation.controller;

import com.cinema.reservation.dto.ReservationCreateRequest;
import com.cinema.reservation.entity.Reservation;
import com.cinema.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation management API")
public class ReservationController {

    private final ReservationService reservationService;

    @PreAuthorize("hasAuthority('ROLE_USER') || hasAuthority('ROLE_ADMIN')")
    @PostMapping
    @Operation(summary = "Create reservation", description = "Creates a new reservation")
    public ResponseEntity<Reservation> createReservation(@RequestBody ReservationCreateRequest request) {
        Reservation reservation = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID", description = "Returns reservation details")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        return reservationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/confirmation/{code}")
    @Operation(summary = "Get reservation by confirmation code", description = "Returns reservation by confirmation code")
    public ResponseEntity<Reservation> getReservationByCode(@PathVariable String code) {
        return reservationService.findByConfirmationCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user reservations", description = "Returns all reservations for a user")
    public ResponseEntity<List<Reservation>> getUserReservations(@PathVariable Long userId) {
        List<Reservation> reservations = reservationService.findByUserId(userId);
        return ResponseEntity.ok(reservations);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}/confirm")
    @Operation(summary = "Confirm reservation", description = "Confirms a pending reservation")
    public ResponseEntity<Reservation> confirmReservation(@PathVariable Long id) {
        try {
            Reservation confirmed = reservationService.confirmReservation(id);
            return ResponseEntity.ok(confirmed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel reservation", description = "Cancels a reservation")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        try {
            Reservation cancelled = reservationService.cancelReservation(id);
            return ResponseEntity.ok(cancelled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}