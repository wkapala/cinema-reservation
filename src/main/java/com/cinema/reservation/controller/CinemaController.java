package com.cinema.reservation.controller;

import com.cinema.reservation.entity.Cinema;
import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.service.CinemaService;
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
@RequestMapping("/api/cinemas")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
@Tag(name = "Cinemas", description = "Cinema management API")
public class CinemaController {

    private final CinemaService cinemaService;

    @GetMapping
    @Operation(summary = "Get all cinemas", description = "Returns a list of all cinemas")
    public ResponseEntity<List<Cinema>> getAllCinemas() {
        List<Cinema> cinemas = cinemaService.findAll();
        return ResponseEntity.ok(cinemas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cinema by ID", description = "Returns cinema details")
    public ResponseEntity<Cinema> getCinemaById(@PathVariable Long id) {
        return cinemaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get cinemas by city", description = "Returns cinemas in specific city")
    public ResponseEntity<List<Cinema>> getCinemasByCity(@PathVariable String city) {
        List<Cinema> cinemas = cinemaService.findByCity(city);
        return ResponseEntity.ok(cinemas);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Create cinema", description = "Creates a new cinema")
    public ResponseEntity<Cinema> createCinema(@RequestBody Cinema cinema) {
        Cinema created = cinemaService.createCinema(cinema);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update cinema", description = "Updates cinema information")
    public ResponseEntity<Cinema> updateCinema(@PathVariable Long id, @RequestBody Cinema cinema) {
        try {
            Cinema updated = cinemaService.updateCinema(id, cinema);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cinema", description = "Deletes a cinema")
    public ResponseEntity<Void> deleteCinema(@PathVariable Long id) {
        try {
            cinemaService.deleteCinema(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{cinemaId}/halls")
    @Operation(summary = "Get cinema halls", description = "Returns all halls for a cinema")
    public ResponseEntity<List<CinemaHall>> getCinemaHalls(@PathVariable Long cinemaId) {
        List<CinemaHall> halls = cinemaService.findHallsByCinemaId(cinemaId);
        return ResponseEntity.ok(halls);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{cinemaId}/halls")
    @Operation(summary = "Add hall to cinema", description = "Creates a new hall in cinema")
    public ResponseEntity<CinemaHall> addHallToCinema(@PathVariable Long cinemaId, @RequestBody CinemaHall hall) {
        return cinemaService.findById(cinemaId)
                .map(cinema -> {
                    hall.setCinema(cinema);
                    CinemaHall created = cinemaService.createCinemaHall(hall);
                    return ResponseEntity.status(HttpStatus.CREATED).body(created);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
