package com.mvp.artplatform.event;

import com.mvp.artplatform.domain.TourGenerationProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Listens for and manages tour generation progress events.
 * This class maintains the state of all active tour generations
 * and provides methods to query their status.
 */
@Component
@Slf4j
public class TourProgressListener {
    // Store active generations with their progress
    private final Map<String, TourGenerationProgress> activeGenerations = new ConcurrentHashMap<>();

    /**
     * Records progress during tour generation.
     * We can call this directly from TourService - no need for a separate event.
     */
    public void updateProgress(String requestId, double progress, String currentTask) {
        TourGenerationProgress tracking = activeGenerations.get(requestId);
        if (tracking != null) {
            tracking.update(progress, currentTask);

            // If complete, schedule for cleanup
            if (tracking.isComplete()) {
                scheduleForCleanup(requestId);
            }
        }
    }

    /**
     * Starts tracking a new tour generation
     */
    public void startTracking(String requestId, String visitorId) {
        TourGenerationProgress progress = new TourGenerationProgress(requestId, visitorId);
        activeGenerations.put(requestId, progress);
    }

    /**
     * Gets current progress of a tour generation
     */
    public Optional<TourGenerationProgress> getProgress(String requestId) {
        return Optional.ofNullable(activeGenerations.get(requestId));
    }

    /**
     * Removes tracking for completed or canceled generations
     */
    private void scheduleForCleanup(String requestId) {
        // Remove after a delay to allow final status checks
        CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> activeGenerations.remove(requestId));
    }
}