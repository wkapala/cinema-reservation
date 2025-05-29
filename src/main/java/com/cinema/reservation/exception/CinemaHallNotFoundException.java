package com.cinema.reservation.exception;

public class CinemaHallNotFoundException extends RuntimeException {
    public CinemaHallNotFoundException(String message) {
        super(message);
    }
}