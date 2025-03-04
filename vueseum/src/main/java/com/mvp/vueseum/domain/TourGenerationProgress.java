package com.mvp.vueseum.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents the progress of a tour generation request.
 * This single class handles both internal tracking and user-facing status.
 */
@Data
public class TourGenerationProgress {
    // Identifies this specific tour generation request
    private final String requestId;

    // Who requested this tour generation
    private final String visitorId;

    // When the generation started
    private final LocalDateTime startedAt;

    // Basic progress tracking
    private double progress;
    private String currentTask;

    private boolean hasError;
    private String errorMessage;

    private String stage;

    public TourGenerationProgress(String requestId, String visitorId) {
        this.requestId = requestId;
        this.visitorId = visitorId;
        this.startedAt = LocalDateTime.now();
        this.progress = 0.0;
        this.currentTask = "Starting tour generation...";
    }

    public void update(double progress, String currentTask) {
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException(
                    "Progress must be between 0.0 and 1.0"
            );
        }
        this.progress = progress;
        this.currentTask = currentTask;
    }

    public void setError(String message) {
        this.hasError = true;
        this.errorMessage = message;
        this.progress = 1.0;
    }
}