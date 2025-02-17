package com.mvp.vueseum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiClientException.class)
    public ResponseEntity<StandardizedErrorResponse> handleApiClientException(ApiClientException e) {
        log.error("External API error: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "External API Error",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardizedErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("Resource not found: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<StandardizedErrorResponse> handleInvalidRequestException(InvalidRequestException e) {
        log.error("Invalid request: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<StandardizedErrorResponse> handleExternalServiceException(PersistenceException e) {
        log.error("Database operation failed: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database error",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GenerationLimitExceededException.class)
    public ResponseEntity<StandardizedErrorResponse> handleGenerationLimitExceededException(GenerationLimitExceededException e) {
        log.error("Too many attempts at generating tours in a given period: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Tour generation limit exceeded",
                e.getMessage()

        );

        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<StandardizedErrorResponse> handleAiProviderException(AiProviderException e) {
        log.error("AiProvider error: {}", e.getMessage(), e);

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI provider is currently unavailable",
                e.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AiProviderAuthException.class)
    public ResponseEntity<StandardizedErrorResponse> handleAiAuthException (
            AiProviderAuthException e) {
        log.error("AI provider authentication error: {}", e.getMessage());

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI service configuration error",
                "Unable to authenticate with AI service"
        );

        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AiProviderRateLimitException.class)
    public ResponseEntity<StandardizedErrorResponse> handleAiProviderRateLimitException (
            AiProviderRateLimitException e) {
        log.warn("AI provider rate limit exceeded: {}", e.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.add(
                "Retry-After",
                String.valueOf(e.getRetryAfter().toSeconds())
        );

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Rate limit exceeded",
                "Please try again after " + e.getRetryAfter().toSeconds() + " seconds"
        );

        return new ResponseEntity<>(error, headers, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(TourLimitExceededException.class)
    public ResponseEntity<StandardizedErrorResponse> handleTourGenerationLimitExceededException (
            TourLimitExceededException e) {
        log.warn("Maximum number of tours in storage reached: {}", e.getMessage());

        StandardizedErrorResponse error = new StandardizedErrorResponse(
                HttpStatus.INSUFFICIENT_STORAGE.value(),
                "Tour storage limit exceeded",
                e.getMessage()

        );

        return new ResponseEntity<>(error, HttpStatus.INSUFFICIENT_STORAGE);
    }
}
