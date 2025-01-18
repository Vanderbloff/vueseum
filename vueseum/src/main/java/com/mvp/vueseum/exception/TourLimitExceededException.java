package com.mvp.vueseum.exception;

public class TourLimitExceededException extends RuntimeException {
    public TourLimitExceededException(String message) {
        super(message);
    }
    public TourLimitExceededException(String message, Throwable cause) {super(message, cause);}
}