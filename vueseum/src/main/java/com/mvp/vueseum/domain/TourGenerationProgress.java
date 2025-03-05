package com.mvp.vueseum.domain;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents the progress of a tour generation request.
 * This single class handles both internal tracking and user-facing status.
 */
@Data
public class TourGenerationProgress {
    private final String requestId;
    private final String visitorId;
    private final LocalDateTime startedAt;

    private double progress;
    private String stage;

    private Integer currentStopIndex;
    private Integer totalStops;

    private boolean hasError;
    private String errorMessage;

    public TourGenerationProgress(String requestId, String visitorId) {
        this.requestId = requestId;
        this.visitorId = visitorId;
        this.startedAt = LocalDateTime.now();
        this.progress = 0.0;
        this.stage = "selecting";
    }

    public void update(double progress, String stage, Integer currentStopIndex, Integer totalStops) {
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("Progress must be between 0.0 and 1.0");
        }
        this.progress = progress;
        this.stage = stage;
        this.currentStopIndex = currentStopIndex;
        this.totalStops = totalStops;
    }

    public void update(double progress, String stage) {
        update(progress, stage, null, null);
    }

    public void setError(String message) {
        this.hasError = true;
        this.errorMessage = message;
        this.progress = 1.0;
        this.stage = "complete";
    }
}