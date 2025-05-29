package com.cinema.reservation.service;

import com.cinema.reservation.dto.ReservationCreateRequest;
import com.cinema.reservation.dto.ReservationStatistics;
import com.cinema.reservation.dto.SeatRequest;
import com.cinema.reservation.entity.*;
import com.cinema.reservation.exception.*;
import com.cinema.reservation.repository.ReservationRepository;
import com.cinema.reservation.repository.ReservedSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservedSeatRepository reservedSeatRepository;

    @Mock
    private ScreeningService screeningService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReservationService reservationService;

    private ReservationCreateRequest reservationRequest;
    private User testUser;
    private Screening testScreening;
    private Reservation testReservation;
    private Movie testMovie;
    private CinemaHall testHall;

    @BeforeEach
    void setUp() {
        // Setup test movie
        testMovie = new Movie();
        testMovie.setId(1L);
        testMovie.setTitle("Test Movie");

        // Setup test hall
        testHall = new CinemaHall();
        testHall.setId(1L);
        testHall.setName("Hall 1");

        // Setup test user
        testUser = new RegularUser();
        testUser.setId(1L);
        testUser.setEmail("user@test.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        // Setup test screening
        testScreening = new Screening();
        testScreening.setId(1L);
        testScreening.setMovie(testMovie);
        testScreening.setHall(testHall);
        testScreening.setStartTime(LocalDateTime.now().plusDays(1));
        testScreening.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        testScreening.setPrice(new BigDecimal("50.00"));

        // Setup reservation request
        SeatRequest seat1 = new SeatRequest();
        seat1.setRowNumber(1);
        seat1.setSeatNumber(5);

        SeatRequest seat2 = new SeatRequest();
        seat2.setRowNumber(1);
        seat2.setSeatNumber(6);

        reservationRequest = new ReservationCreateRequest();
        reservationRequest.setUserId(1L);
        reservationRequest.setScreeningId(1L);
        reservationRequest.setSeats(Arrays.asList(seat1, seat2));

        // Setup test reservation
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setScreening(testScreening);
        testReservation.setTotalPrice(new BigDecimal("100.00"));
        testReservation.setStatus(Reservation.ReservationStatus.PENDING);
        testReservation.setConfirmationCode(UUID.randomUUID().toString());
    }

    @Test
    void createReservation_ValidRequest_Success() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(screeningService.findById(1L)).thenReturn(Optional.of(testScreening));
        when(reservedSeatRepository.existsByScreeningIdAndRowNumberAndSeatNumber(anyLong(), anyInt(), anyInt()))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        when(reservedSeatRepository.saveAll(anyList())).thenReturn(Arrays.asList(new ReservedSeat(), new ReservedSeat()));
        when(screeningService.reserveSeats(1L, 2)).thenReturn(true);

        // When
        Reservation result = reservationService.createReservation(reservationRequest);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testScreening, result.getScreening());
        assertEquals(new BigDecimal("100.00"), result.getTotalPrice());

        verify(userService).findById(1L);
        verify(screeningService).findById(1L);
        verify(reservedSeatRepository, times(2)).existsByScreeningIdAndRowNumberAndSeatNumber(anyLong(), anyInt(), anyInt());
        verify(reservationRepository).save(any(Reservation.class));
        verify(reservedSeatRepository).saveAll(anyList());
        verify(screeningService).reserveSeats(1L, 2);
    }

    @Test
    void createReservation_NullUserId_ThrowsException() {
        // Given
        reservationRequest.setUserId(null);

        // When & Then
        assertThrows(InvalidReservationDataException.class,
                () -> reservationService.createReservation(reservationRequest));

        verify(userService, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_NullScreeningId_ThrowsException() {
        // Given
        reservationRequest.setScreeningId(null);

        // When & Then
        assertThrows(InvalidReservationDataException.class,
                () -> reservationService.createReservation(reservationRequest));
    }

    @Test
    void createReservation_NoSeats_ThrowsException() {
        // Given
        reservationRequest.setSeats(Arrays.asList());

        // When & Then
        assertThrows(InvalidReservationDataException.class,
                () -> reservationService.createReservation(reservationRequest));
    }

    @Test
    void createReservation_TooManySeats_ThrowsException() {
        // Given
        List<SeatRequest> tooManySeats = Arrays.asList(
                new SeatRequest(), new SeatRequest(), new SeatRequest(),
                new SeatRequest(), new SeatRequest(), new SeatRequest(),
                new SeatRequest(), new SeatRequest(), new SeatRequest(),
                new SeatRequest(), new SeatRequest() // 11 seats
        );
        reservationRequest.setSeats(tooManySeats);

        // When & Then
        InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
                () -> reservationService.createReservation(reservationRequest));
        assertTrue(exception.getMessage().contains("Cannot reserve more than 10 seats"));
    }

    @Test
    void createReservation_UserNotFound_ThrowsException() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> reservationService.createReservation(reservationRequest));

        verify(screeningService, never()).findById(any());
    }

    @Test
    void createReservation_ScreeningNotFound_ThrowsException() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(screeningService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ScreeningNotFoundException.class,
                () -> reservationService.createReservation(reservationRequest));
    }

    @Test
    void createReservation_SeatAlreadyReserved_ThrowsException() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(screeningService.findById(1L)).thenReturn(Optional.of(testScreening));
        when(reservedSeatRepository.existsByScreeningIdAndRowNumberAndSeatNumber(1L, 1, 5))
                .thenReturn(true); // First seat is already reserved

        // When & Then
        SeatNotAvailableException exception = assertThrows(SeatNotAvailableException.class,
                () -> reservationService.createReservation(reservationRequest));
        assertTrue(exception.getMessage().contains("Seat 1-5 is already reserved"));
    }

    @Test
    void findById_ExistingReservation_ReturnsReservation() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When
        Optional<Reservation> result = reservationService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testReservation, result.get());
    }

    @Test
    void findByConfirmationCode_ExistingReservation_ReturnsReservation() {
        // Given
        String confirmationCode = "ABC123";
        when(reservationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(testReservation));

        // When
        Optional<Reservation> result = reservationService.findByConfirmationCode(confirmationCode);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testReservation, result.get());
    }

    @Test
    void findByUserId_ReturnsUserReservations() {
        // Given
        List<Reservation> userReservations = Arrays.asList(testReservation);
        when(reservationRepository.findByUserId(1L)).thenReturn(userReservations);

        // When
        List<Reservation> result = reservationService.findByUserId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(testReservation, result.get(0));
    }

    @Test
    void findByUserId_WithPageable_ReturnsPagedReservations() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Reservation> pagedReservations = new PageImpl<>(Arrays.asList(testReservation));
        when(reservationRepository.findByUserId(1L, pageable)).thenReturn(pagedReservations);

        // When
        Page<Reservation> result = reservationService.findByUserId(1L, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(testReservation, result.getContent().get(0));
    }

    @Test
    void confirmReservation_PendingReservation_Success() {
        // Given
        testReservation.setStatus(Reservation.ReservationStatus.PENDING);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // When
        Reservation result = reservationService.confirmReservation(1L);

        // Then
        assertEquals(Reservation.ReservationStatus.CONFIRMED, result.getStatus());
        verify(reservationRepository).save(testReservation);
    }

    @Test
    void confirmReservation_NonExistingReservation_ThrowsException() {
        // Given
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ReservationNotFoundException.class,
                () -> reservationService.confirmReservation(99L));
    }

    @Test
    void confirmReservation_AlreadyConfirmed_ThrowsException() {
        // Given
        testReservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        assertThrows(InvalidReservationStateException.class,
                () -> reservationService.confirmReservation(1L));
    }

    @Test
    void cancelReservation_ValidReservation_Success() {
        // Given
        testReservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        testReservation.setReservedSeats(Arrays.asList(new ReservedSeat(), new ReservedSeat()));
        testScreening.setStartTime(LocalDateTime.now().plusDays(1)); // More than 2 hours away

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);
        when(screeningService.reserveSeats(1L, -2)).thenReturn(true);

        // When
        Reservation result = reservationService.cancelReservation(1L);

        // Then
        assertEquals(Reservation.ReservationStatus.CANCELLED, result.getStatus());
        verify(screeningService).reserveSeats(1L, -2);
        verify(reservationRepository).save(testReservation);
    }

    @Test
    void cancelReservation_AlreadyCancelled_ThrowsException() {
        // Given
        testReservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        assertThrows(InvalidReservationStateException.class,
                () -> reservationService.cancelReservation(1L));
    }

    @Test
    void cancelReservation_TooCloseToScreening_ThrowsException() {
        // Given
        testReservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        testScreening.setStartTime(LocalDateTime.now().plusHours(1)); // Less than 2 hours away

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        assertThrows(ReservationCancellationException.class,
                () -> reservationService.cancelReservation(1L));
    }

    @Test
    void getReservationStatistics_ReturnsCorrectStats() {
        // Given
        when(reservationRepository.countTodayConfirmedReservations()).thenReturn(5L);
        when(reservationRepository.calculateRevenue(any(LocalDateTime.class))).thenReturn(1500.0);
        when(reservationRepository.findRecentReservations(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testReservation, testReservation, testReservation));

        // When
        ReservationStatistics result = reservationService.getReservationStatistics();

        // Then
        assertNotNull(result);
        assertEquals(5L, result.getConfirmedTodayCount());
        assertEquals(new BigDecimal("1500.0"), result.getMonthlyRevenue());
        assertEquals(3L, result.getWeeklyReservationsCount());
    }

    @Test
    void getReservationStatistics_NullRevenue_ReturnsZero() {
        // Given
        when(reservationRepository.countTodayConfirmedReservations()).thenReturn(0L);
        when(reservationRepository.calculateRevenue(any(LocalDateTime.class))).thenReturn(null);
        when(reservationRepository.findRecentReservations(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // When
        ReservationStatistics result = reservationService.getReservationStatistics();

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getConfirmedTodayCount());
        assertEquals(BigDecimal.ZERO, result.getMonthlyRevenue());
        assertEquals(0L, result.getWeeklyReservationsCount());
    }
}