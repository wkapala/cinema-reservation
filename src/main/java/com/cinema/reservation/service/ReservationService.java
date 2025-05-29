package com.cinema.reservation.service;

import com.cinema.reservation.dto.ReservationCreateRequest;
import com.cinema.reservation.dto.ReservationStatistics;
import com.cinema.reservation.dto.SeatRequest;
import com.cinema.reservation.entity.Reservation;
import com.cinema.reservation.entity.ReservedSeat;
import com.cinema.reservation.entity.Screening;
import com.cinema.reservation.entity.User;
import com.cinema.reservation.exception.InvalidReservationDataException;
import com.cinema.reservation.exception.InvalidReservationStateException;
import com.cinema.reservation.exception.ReservationCancellationException;
import com.cinema.reservation.exception.ReservationNotFoundException;
import com.cinema.reservation.exception.SeatNotAvailableException;
import com.cinema.reservation.exception.ScreeningNotFoundException;
import com.cinema.reservation.exception.UserNotFoundException;
import com.cinema.reservation.repository.ReservationRepository;
import com.cinema.reservation.repository.ReservedSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ScreeningService screeningService;
    private final UserService userService;

    @Transactional
    public Reservation createReservation(ReservationCreateRequest request) {
        log.info("Creating reservation for user {} and screening {}",
                request.getUserId(), request.getScreeningId());

        validateReservationRequest(request);

        User user = getUserOrThrow(request.getUserId());
        Screening screening = getScreeningOrThrow(request.getScreeningId());

        validateSeatAvailability(request.getScreeningId(), request.getSeats());

        Reservation reservation = buildReservation(user, screening, request);
        Reservation savedReservation = reservationRepository.save(reservation);

        List<ReservedSeat> reservedSeats = createReservedSeats(savedReservation, request.getSeats());
        reservedSeatRepository.saveAll(reservedSeats);
        savedReservation.setReservedSeats(reservedSeats);

        screeningService.reserveSeats(request.getScreeningId(), request.getSeats().size());

        log.info("Reservation created with ID: {} and confirmation code: {}",
                savedReservation.getId(), savedReservation.getConfirmationCode());

        return savedReservation;
    }

    public Optional<Reservation> findById(Long id) {
        return reservationRepository.findById(id);
    }

    public Optional<Reservation> findByConfirmationCode(String confirmationCode) {
        return reservationRepository.findByConfirmationCode(confirmationCode);
    }

    public List<Reservation> findByUserId(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    public Page<Reservation> findByUserId(Long userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable);
    }

    public List<Reservation> findByScreeningId(Long screeningId) {
        return reservationRepository.findByScreeningId(screeningId);
    }

    public List<Reservation> findByStatus(Reservation.ReservationStatus status) {
        return reservationRepository.findByStatus(status);
    }

    public List<Reservation> findUserReservationsWithDetails(Long userId) {
        return reservationRepository.findUserReservationsWithDetails(userId);
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
            throw new InvalidReservationStateException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);

        log.info("Reservation {} confirmed", reservationId);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new InvalidReservationStateException("Reservation is already cancelled");
        }

        if (canCancelReservation(reservation)) {
            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);

            screeningService.reserveSeats(reservation.getScreening().getId(),
                    -reservation.getReservedSeats().size());

            log.info("Reservation {} cancelled", reservationId);
        } else {
            throw new ReservationCancellationException("Cannot cancel reservation - screening too soon");
        }

        return reservationRepository.save(reservation);
    }

    public ReservationStatistics getReservationStatistics() {
        long confirmedToday = reservationRepository.countTodayConfirmedReservations();
        Double revenue = reservationRepository.calculateRevenue(LocalDateTime.now().minusDays(30));
        List<Reservation> recent = reservationRepository.findRecentReservations(LocalDateTime.now().minusDays(7));

        return ReservationStatistics.builder()
                .confirmedTodayCount(confirmedToday)
                .monthlyRevenue(revenue != null ? BigDecimal.valueOf(revenue) : BigDecimal.ZERO)
                .weeklyReservationsCount((long) recent.size())
                .build();
    }

    private void validateReservationRequest(ReservationCreateRequest request) {
        if (request.getUserId() == null) {
            throw new InvalidReservationDataException("User ID cannot be null");
        }

        if (request.getScreeningId() == null) {
            throw new InvalidReservationDataException("Screening ID cannot be null");
        }

        if (request.getSeats() == null || request.getSeats().isEmpty()) {
            throw new InvalidReservationDataException("At least one seat must be selected");
        }

        if (request.getSeats().size() > 10) {
            throw new InvalidReservationDataException("Cannot reserve more than 10 seats at once");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    private Screening getScreeningOrThrow(Long screeningId) {
        return screeningService.findById(screeningId)
                .orElseThrow(() -> new ScreeningNotFoundException("Screening not found with ID: " + screeningId));
    }

    private void validateSeatAvailability(Long screeningId, List<SeatRequest> seats) {
        for (SeatRequest seat : seats) {
            boolean isOccupied = reservedSeatRepository.existsByScreeningIdAndRowNumberAndSeatNumber(
                    screeningId, seat.getRowNumber(), seat.getSeatNumber());

            if (isOccupied) {
                throw new SeatNotAvailableException(
                        String.format("Seat %d-%d is already reserved", seat.getRowNumber(), seat.getSeatNumber()));
            }
        }
    }

    private Reservation buildReservation(User user, Screening screening, ReservationCreateRequest request) {
        BigDecimal totalPrice = screening.getPrice().multiply(BigDecimal.valueOf(request.getSeats().size()));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setScreening(screening);
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(Reservation.ReservationStatus.PENDING);

        return reservation;
    }

    private List<ReservedSeat> createReservedSeats(Reservation reservation, List<SeatRequest> seats) {
        return seats.stream()
                .map(seat -> {
                    ReservedSeat reservedSeat = new ReservedSeat();
                    reservedSeat.setReservation(reservation);
                    reservedSeat.setScreening(reservation.getScreening());
                    reservedSeat.setRowNumber(seat.getRowNumber());
                    reservedSeat.setSeatNumber(seat.getSeatNumber());
                    return reservedSeat;
                })
                .toList();
    }

    private boolean canCancelReservation(Reservation reservation) {
        LocalDateTime twoHoursBefore = reservation.getScreening().getStartTime().minusHours(2);
        return LocalDateTime.now().isBefore(twoHoursBefore);
    }
}