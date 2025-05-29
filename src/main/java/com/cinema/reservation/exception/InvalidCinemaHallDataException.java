package com.cinema.reservation.exception;

public class InvalidCinemaHallDataException extends RuntimeException {
    public InvalidCinemaHallDataException(String message) {
        super(message);
    }
}