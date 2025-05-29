package com.cinema.reservation.exception;

public class InvalidCinemaDataException extends RuntimeException {
    public InvalidCinemaDataException(String message) {
        super(message);
    }
}