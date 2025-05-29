package com.cinema.reservation.service;

import com.cinema.reservation.entity.Cinema;
import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.exception.CinemaNotFoundException;
import com.cinema.reservation.exception.CinemaHallNotFoundException;
import com.cinema.reservation.exception.InvalidCinemaDataException;
import com.cinema.reservation.exception.InvalidCinemaHallDataException;
import com.cinema.reservation.repository.CinemaHallRepository;
import com.cinema.reservation.repository.CinemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CinemaService {

    // Dependency Inversion
    private final CinemaRepository cinemaRepository;
    private final CinemaHallRepository cinemaHallRepository;

    // Single Responsibility - zarządzanie kinami i salami

    public Cinema createCinema(Cinema cinema) {
        log.info("Creating new cinema: {}", cinema.getName());

        validateCinemaData(cinema);
        Cinema savedCinema = cinemaRepository.save(cinema);

        log.info("Cinema created with ID: {}", savedCinema.getId());
        return savedCinema;
    }

    public Optional<Cinema> findById(Long id) {
        return cinemaRepository.findById(id);
    }

    public List<Cinema> findAll() {
        return cinemaRepository.findAll();
    }

    public List<Cinema> findByCity(String city) {
        return cinemaRepository.findByCity(city);
    }

    public List<Cinema> searchByName(String name) {
        return cinemaRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Cinema> findCinemasWithHallsByCity(String city) {
        return cinemaRepository.findCinemasWithHallsByCity(city);
    }

    public Cinema updateCinema(Long id, Cinema cinemaUpdates) {
        Cinema existingCinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new CinemaNotFoundException("Cinema not found with ID: " + id));

        existingCinema.setName(cinemaUpdates.getName());
        existingCinema.setAddress(cinemaUpdates.getAddress());
        existingCinema.setCity(cinemaUpdates.getCity());
        existingCinema.setPhoneNumber(cinemaUpdates.getPhoneNumber());

        return cinemaRepository.save(existingCinema);
    }

    public void deleteCinema(Long id) {
        if (!cinemaRepository.existsById(id)) {
            throw new CinemaNotFoundException("Cinema not found with ID: " + id);
        }

        cinemaRepository.deleteById(id);
        log.info("Cinema deleted with ID: {}", id);
    }

    // Cinema Hall operations
    public CinemaHall createCinemaHall(CinemaHall hall) {
        log.info("Creating new cinema hall: {} in cinema ID: {}",
                hall.getName(), hall.getCinema().getId());

        validateCinemaHallData(hall);
        CinemaHall savedHall = cinemaHallRepository.save(hall);

        log.info("Cinema hall created with ID: {}", savedHall.getId());
        return savedHall;
    }

    public Optional<CinemaHall> findHallById(Long id) {
        return cinemaHallRepository.findById(id);
    }

    public List<CinemaHall> findHallsByCinemaId(Long cinemaId) {
        return cinemaHallRepository.findByCinemaId(cinemaId);
    }

    public List<CinemaHall> findHallsByType(CinemaHall.HallType hallType) {
        return cinemaHallRepository.findByHallType(hallType);
    }

    public List<CinemaHall> findHallsWithMinimumCapacity(Integer minSeats) {
        return cinemaHallRepository.findHallsWithMinimumCapacity(minSeats);
    }

    public List<CinemaHall> findHallsByCinemaAndType(Long cinemaId, CinemaHall.HallType hallType) {
        return cinemaHallRepository.findByCinemaIdAndHallType(cinemaId, hallType);
    }

    public CinemaHall updateCinemaHall(Long id, CinemaHall hallUpdates) {
        CinemaHall existingHall = cinemaHallRepository.findById(id)
                .orElseThrow(() -> new CinemaHallNotFoundException("Cinema hall not found with ID: " + id));

        existingHall.setName(hallUpdates.getName());
        existingHall.setTotalSeats(hallUpdates.getTotalSeats());
        existingHall.setRows(hallUpdates.getRows());
        existingHall.setSeatsPerRow(hallUpdates.getSeatsPerRow());
        existingHall.setHallType(hallUpdates.getHallType());

        return cinemaHallRepository.save(existingHall);
    }

    public void deleteCinemaHall(Long id) {
        if (!cinemaHallRepository.existsById(id)) {
            throw new CinemaHallNotFoundException("Cinema hall not found with ID: " + id);
        }

        cinemaHallRepository.deleteById(id);
        log.info("Cinema hall deleted with ID: {}", id);
    }

    // Business validation
    private void validateCinemaData(Cinema cinema) {
        if (cinema.getName() == null || cinema.getName().trim().isEmpty()) {
            throw new InvalidCinemaDataException("Cinema name cannot be empty");
        }

        if (cinema.getAddress() == null || cinema.getAddress().trim().isEmpty()) {
            throw new InvalidCinemaDataException("Cinema address cannot be empty");
        }
    }

    private void validateCinemaHallData(CinemaHall hall) {
        if (hall.getName() == null || hall.getName().trim().isEmpty()) {
            throw new InvalidCinemaHallDataException("Hall name cannot be empty");
        }

        if (hall.getTotalSeats() == null || hall.getTotalSeats() <= 0) {
            throw new InvalidCinemaHallDataException("Total seats must be positive");
        }

        if (hall.getRows() == null || hall.getRows() <= 0) {
            throw new InvalidCinemaHallDataException("Number of rows must be positive");
        }

        if (hall.getSeatsPerRow() == null || hall.getSeatsPerRow() <= 0) {
            throw new InvalidCinemaHallDataException("Seats per row must be positive");
        }

        // Business rule - sprawdź czy totalSeats = rows * seatsPerRow
        int calculatedSeats = hall.getRows() * hall.getSeatsPerRow();
        if (!hall.getTotalSeats().equals(calculatedSeats)) {
            throw new InvalidCinemaHallDataException(
                    "Total seats must equal rows * seats per row");
        }
    }
}