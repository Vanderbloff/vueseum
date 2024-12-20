package com.mvp.artplatform.exception;

public class GenerationLimitExceededException extends RuntimeException {
    public GenerationLimitExceededException(String message) {
        super(message);
    }
    public GenerationLimitExceededException(String message, Throwable cause) { super(message, cause); }
}
