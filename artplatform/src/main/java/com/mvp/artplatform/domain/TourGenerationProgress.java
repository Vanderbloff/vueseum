package com.mvp.artplatform.domain;

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
    private double progress;  // 0.0 to 1.0
    private String currentTask;  // E.g., "Selecting artworks", "Generating descriptions"
    private boolean isComplete;

    // Keep track of when we last updated this progress
    private LocalDateTime lastUpdated;

    public TourGenerationProgress(String requestId, String visitorId) {
        this.requestId = requestId;
        this.visitorId = visitorId;
        this.startedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.progress = 0.0;
        this.currentTask = "Starting tour generation...";
        this.isComplete = false;
    }

    /**
     * Updates the progress of tour generation
     */
    public void update(double progress, String currentTask) {
        this.progress = progress;
        this.currentTask = currentTask;
        this.lastUpdated = LocalDateTime.now();
        this.isComplete = progress >= 1.0;
    }
}