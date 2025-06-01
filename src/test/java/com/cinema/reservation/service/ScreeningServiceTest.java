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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private CinemaHallRepository cinemaHallRepository;

    @InjectMocks
    private ScreeningService screeningService;

    private Screening testScreening;
    private Movie testMovie;
    private CinemaHall testHall;

    @BeforeEach
    void setUp() {
        testMovie = new Movie();
        testMovie.setId(1L);
        testMovie.setTitle("Test Movie");

        testHall = new CinemaHall();
        testHall.setId(1L);
        testHall.setName("Hall 1");
        testHall.setTotalSeats(100);

        testScreening = new Screening();
        testScreening.setId(1L);
        testScreening.setMovie(testMovie);
        testScreening.setHall(testHall);
        testScreening.setStartTime(LocalDateTime.now().plusDays(1));
        testScreening.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        testScreening.setPrice(new BigDecimal("50.00"));
        testScreening.setAvailableSeats(100);
    }

    @Test
    void createScreeningFromRequest_ValidData_Success() {
        ScreeningCreateRequest request = new ScreeningCreateRequest();
        request.setMovieId(1L);
        request.setHallId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setPrice(BigDecimal.valueOf(50));

        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(cinemaHallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(screeningRepository.findConflictingScreenings(anyLong(), any(), any())).thenReturn(List.of());
        when(screeningRepository.save(any(Screening.class))).thenReturn(testScreening);

        Screening result = screeningService.createScreeningFromRequest(request);

        assertNotNull(result);
        assertEquals(testMovie, result.getMovie());
        verify(screeningRepository).save(any(Screening.class));
    }

    @Test
    void findById_ExistingScreening_ReturnsScreening() {
        when(screeningRepository.findById(1L)).thenReturn(Optional.of(testScreening));
        Optional<Screening> result = screeningService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals(testScreening, result.get());
    }

    @Test
    void reserveSeats_AvailableSeats_Success() {
        when(screeningRepository.findById(1L)).thenReturn(Optional.of(testScreening));
        when(screeningRepository.save(any())).thenReturn(testScreening);
        boolean result = screeningService.reserveSeats(1L, 5);
        assertTrue(result);
        assertEquals(95, testScreening.getAvailableSeats());
        verify(screeningRepository).save(testScreening);
    }

    @Test
    void reserveSeats_NotEnoughSeats_ReturnsFalse() {
        testScreening.setAvailableSeats(3);
        when(screeningRepository.findById(1L)).thenReturn(Optional.of(testScreening));
        boolean result = screeningService.reserveSeats(1L, 5);
        assertFalse(result);
        verify(screeningRepository, never()).save(any());
    }

    @Test
    void deleteScreening_ExistingScreening_Success() {
        when(screeningRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> screeningService.deleteScreening(1L));
        verify(screeningRepository).deleteById(1L);
    }
}
