package com.cinema.reservation.service;

import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.entity.Movie;
import com.cinema.reservation.entity.Screening;
import com.cinema.reservation.exception.InvalidScreeningDataException;
import com.cinema.reservation.exception.ScreeningConflictException;
import com.cinema.reservation.exception.ScreeningNotFoundException;
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
    void createScreening_ValidData_Success() {
        // Given
        when(screeningRepository.findConflictingScreenings(anyLong(), any(), any()))
                .thenReturn(Arrays.asList());
        when(screeningRepository.save(any(Screening.class))).thenReturn(testScreening);

        // When
        Screening result = screeningService.createScreening(testScreening);

        // Then
        assertNotNull(result);
        assertEquals(testMovie, result.getMovie());
        verify(screeningRepository).save(testScreening);
    }

    @Test
    void createScreening_NullStartTime_ThrowsException() {
        // Given
        testScreening.setStartTime(null);

        // When & Then
        assertThrows(InvalidScreeningDataException.class,
                () -> screeningService.createScreening(testScreening));
    }

    @Test
    void createScreening_StartTimeInPast_ThrowsException() {
        // Given
        testScreening.setStartTime(LocalDateTime.now().minusDays(1));

        // When & Then
        assertThrows(InvalidScreeningDataException.class,
                () -> screeningService.createScreening(testScreening));
    }

    @Test
    void createScreening_ConflictingScreening_ThrowsException() {
        // Given
        when(screeningRepository.findConflictingScreenings(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(new Screening()));

        // When & Then
        assertThrows(ScreeningConflictException.class,
                () -> screeningService.createScreening(testScreening));
    }

    @Test
    void findById_ExistingScreening_ReturnsScreening() {
        // Given
        when(screeningRepository.findById(1L)).thenReturn(Optional.of(testScreening));

        // When
        Optional<Screening> result = screeningService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testScreening, result.get());
    }

    @Test
    void reserveSeats_AvailableSeats_Success() {
        // Given
        when(screeningRepository.findById(1L)).thenReturn(Optional.of(testScreening));
        when(screeningRepository.save(any())).thenReturn(testScreening);

        // When
        boolean result = screeningService.reserveSeats(1L, 5);

        // Then
        assertTrue(result);
        assertEquals(95, testScreening.getAvailableSeats());
        verify(screeningRepository).save(testScreening);
    }

    @Test
    void reserveSeats_NotEnoughSeats_ReturnsFalse() {
        // Given
        testScreening.setAvailableSeats(3);
        when(screeningRepository.findById(1L)).thenReturn(Optional.of(testScreening));

        // When
        boolean result = screeningService.reserveSeats(1L, 5);

        // Then
        assertFalse(result);
        verify(screeningRepository, never()).save(any());
    }

    @Test
    void deleteScreening_ExistingScreening_Success() {
        // Given
        when(screeningRepository.existsById(1L)).thenReturn(true);

        // When
        assertDoesNotThrow(() -> screeningService.deleteScreening(1L));

        // Then
        verify(screeningRepository).deleteById(1L);
    }
}