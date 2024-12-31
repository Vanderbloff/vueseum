package com.mvp.vueseum.exception;

public class GenerationLimitExceededException extends RuntimeException {
    public GenerationLimitExceededException(String message) {
        super(message);
    }
    public GenerationLimitExceededException(String message, Throwable cause) { super(message, cause); }
}
