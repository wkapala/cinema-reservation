package com.cinema.reservation.exception;

public class InvalidScreeningDataException extends RuntimeException {
    public InvalidScreeningDataException(String message) {
        super(message);
    }
}
