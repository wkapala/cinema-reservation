package com.cinema.reservation.exception;

public class ReservationCancellationException extends RuntimeException {
    public ReservationCancellationException(String message) {
        super(message);
    }
}
