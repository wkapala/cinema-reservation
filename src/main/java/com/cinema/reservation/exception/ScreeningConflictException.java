package com.cinema.reservation.exception;

public class ScreeningConflictException extends RuntimeException {
    public ScreeningConflictException(String message) {
        super(message);
    }
}