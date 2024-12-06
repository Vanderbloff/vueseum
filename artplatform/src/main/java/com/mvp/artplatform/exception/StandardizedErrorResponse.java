package com.mvp.artplatform.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StandardizedErrorResponse {
    // HTTP Status code
    private final int status;

    // Error message for developers
    private final String message;

    // More detailed description of what went wrong
    private final String details;

    // Timestamp of when the error occurred
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public StandardizedErrorResponse(int status, String message, String details) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
}
