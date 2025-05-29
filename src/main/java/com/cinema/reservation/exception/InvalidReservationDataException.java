package com.cinema.reservation.exception;

public class InvalidReservationDataException extends RuntimeException {
    public InvalidReservationDataException(String message) {
        super(message);
    }
}
