package com.cinema.reservation.exception;

public class UserDeactivationException extends RuntimeException {
    public UserDeactivationException(String message) {
        super(message);
    }
}