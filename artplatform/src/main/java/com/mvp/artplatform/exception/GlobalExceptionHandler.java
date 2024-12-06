package com.mvp.artplatform.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final static Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiClientException.class)
    public ResponseEntity<StandardizedErrorResponse> handleApiClientException(ApiClientException e) {
        logger.error("External API error: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "External API Error",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardizedErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        logger.error("Resource not found: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardizedErrorResponse> handleInvalidRequestException(InvalidRequestException e) {
        logger.error("Invalid request: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardizedErrorResponse> handleExternalServiceException(PersistenceException e) {
        logger.error("Database operation failed: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database error",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
