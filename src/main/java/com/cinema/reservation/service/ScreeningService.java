package com.cinema.reservation.service;

import com.cinema.reservation.dto.ScreeningCreateRequest;
import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.entity.Movie;
import com.cinema.reservation.entity.Screening;
import com.cinema.reservation.exception.InvalidScreeningDataException;
import com.cinema.reservation.exception.ScreeningConflictException;
import com.cinema.reservation.exception.ScreeningNotFoundException;
import com.cinema.reservation.repository.CinemaHallRepository;
import com.cinema.reservation.repository.MovieRepository;
import com.cinema.reservation.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final CinemaHallRepository cinemaHallRepository;

    public Screening createScreeningFromRequest(ScreeningCreateRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new InvalidScreeningDataException("Movie not found with ID: " + request.getMovieId()));
        CinemaHall hall = cinemaHallRepository.findById(request.getHallId())
                .orElseThrow(() -> new InvalidScreeningDataException("Cinema hall not found with ID: " + request.getHallId()));

        Screening screening = new Screening();
        screening.setMovie(movie);
        screening.setHall(hall);
        screening.setStartTime(request.getStartTime());
        screening.setEndTime(request.getEndTime());
        screening.setPrice(request.getPrice());
        screening.setAvailableSeats(hall.getTotalSeats());

        validateScreening(screening);
        checkForConflicts(screening);

        Screening savedScreening = screeningRepository.save(screening);
        log.info("Screening created with ID: {}", savedScreening.getId());

        return savedScreening;
    }

    public Optional<Screening> findById(Long id) {
        return screeningRepository.findById(id);
    }

    public List<Screening> findAll() {
        return screeningRepository.findAll();
    }

    public List<Screening> findByMovieId(Long movieId) {
        return screeningRepository.findByMovieId(movieId);
    }

    public List<Screening> findByHallId(Long hallId) {
        return screeningRepository.findByHallId(hallId);
    }

    public List<Screening> findAvailableScreenings() {
        return screeningRepository.findAvailableScreenings(LocalDateTime.now());
    }

    public List<Screening> findByDate(LocalDateTime date) {
        return screeningRepository.findByDate(date);
    }

    public List<Screening> findUpcomingScreeningsForMovie(Long movieId) {
        return screeningRepository.findUpcomingScreeningsForMovie(movieId);
    }

    public List<Screening> findScreeningsWithAvailableSeats(Integer requiredSeats) {
        return screeningRepository.findScreeningsWithAvailableSeats(requiredSeats);
    }

    public boolean hasAvailableSeats(Long screeningId, Integer requiredSeats) {
        Optional<Screening> screeningOpt = screeningRepository.findById(screeningId);

        return screeningOpt.map(screening -> screening.hasAvailableSeats(requiredSeats)).orElse(false);
    }

    @Transactional
    public boolean reserveSeats(Long screeningId, Integer seatsToReserve) {
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ScreeningNotFoundException("Screening not found with ID: " + screeningId));

        if (!screening.hasAvailableSeats(seatsToReserve)) {
            return false;
        }

        screening.reserveSeats(seatsToReserve);
        screeningRepository.save(screening);

        log.info("Reserved {} seats for screening ID: {}", seatsToReserve, screeningId);
        return true;
    }

    public Screening updateScreening(Long id, Screening screeningUpdates) {
        Screening existingScreening = screeningRepository.findById(id)
                .orElseThrow(() -> new ScreeningNotFoundException("Screening not found with ID: " + id));

        if (!existingScreening.getStartTime().equals(screeningUpdates.getStartTime()) ||
                !existingScreening.getHall().getId().equals(screeningUpdates.getHall().getId())) {
            checkForConflicts(screeningUpdates);
        }

        existingScreening.setStartTime(screeningUpdates.getStartTime());
        existingScreening.setEndTime(screeningUpdates.getEndTime());
        existingScreening.setPrice(screeningUpdates.getPrice());

        return screeningRepository.save(existingScreening);
    }

    public void deleteScreening(Long id) {
        if (!screeningRepository.existsById(id)) {
            throw new ScreeningNotFoundException("Screening not found with ID: " + id);
        }

        screeningRepository.deleteById(id);
        log.info("Screening deleted with ID: {}", id);
    }

    private void validateScreening(Screening screening) {
        if (screening.getStartTime() == null) {
            throw new InvalidScreeningDataException("Start time cannot be null");
        }

        if (screening.getEndTime() == null) {
            throw new InvalidScreeningDataException("End time cannot be null");
        }

        if (screening.getStartTime().isAfter(screening.getEndTime())) {
            throw new InvalidScreeningDataException("Start time cannot be after end time");
        }

        if (screening.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidScreeningDataException("Cannot create screening in the past");
        }

        if (screening.getPrice() == null || screening.getPrice().doubleValue() <= 0) {
            throw new InvalidScreeningDataException("Price must be positive");
        }
    }

    private void checkForConflicts(Screening screening) {
        List<Screening> conflictingScreenings = screeningRepository.findConflictingScreenings(
                screening.getHall().getId(),
                screening.getStartTime(),
                screening.getEndTime()
        );

        if (!conflictingScreenings.isEmpty()) {
            throw new ScreeningConflictException(
                    "Screening conflicts with existing screening in the same hall");
        }
    }
}
