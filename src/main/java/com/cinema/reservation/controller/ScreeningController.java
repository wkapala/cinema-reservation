package com.cinema.reservation.controller;

import com.cinema.reservation.dto.ScreeningCreateRequest;
import com.cinema.reservation.entity.Screening;
import com.cinema.reservation.service.ScreeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screenings")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
@Tag(name = "Screenings", description = "Screening management API")
public class ScreeningController {

    private final ScreeningService screeningService;

    @GetMapping
    @Operation(summary = "Get all screenings", description = "Returns all screenings")
    public ResponseEntity<List<Screening>> getAllScreenings() {
        List<Screening> screenings = screeningService.findAll();
        return ResponseEntity.ok(screenings);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get screening by ID", description = "Returns screening details")
    public ResponseEntity<Screening> getScreeningById(@PathVariable Long id) {
        return screeningService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get screenings for movie", description = "Returns all screenings for a specific movie")
    public ResponseEntity<List<Screening>> getScreeningsByMovie(@PathVariable Long movieId) {
        List<Screening> screenings = screeningService.findByMovieId(movieId);
        return ResponseEntity.ok(screenings);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming screenings", description = "Returns all future screenings")
    public ResponseEntity<List<Screening>> getUpcomingScreenings() {
        List<Screening> screenings = screeningService.findAvailableScreenings();
        return ResponseEntity.ok(screenings);
    }

    @GetMapping("/available")
    @Operation(summary = "Get screenings with available seats", description = "Returns screenings with minimum available seats")
    public ResponseEntity<List<Screening>> getAvailableScreenings(@RequestParam(defaultValue = "1") Integer minSeats) {
        List<Screening> screenings = screeningService.findScreeningsWithAvailableSeats(minSeats);
        return ResponseEntity.ok(screenings);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Create screening", description = "Creates a new screening")
    public ResponseEntity<Screening> createScreening(@RequestBody ScreeningCreateRequest request) {
        try {
            Screening created = screeningService.createScreeningFromRequest(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update screening", description = "Updates screening information")
    public ResponseEntity<Screening> updateScreening(@PathVariable Long id, @RequestBody Screening screening) {
        try {
            Screening updated = screeningService.updateScreening(id, screening);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete screening", description = "Deletes a screening")
    public ResponseEntity<Void> deleteScreening(@PathVariable Long id) {
        try {
            screeningService.deleteScreening(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
