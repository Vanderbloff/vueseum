package com.mvp.vueseum.event;

import com.mvp.vueseum.domain.TourGenerationProgress;
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
    private final Map<String, TourGenerationProgress> activeGenerations = new ConcurrentHashMap<>();

    public void initializeProgress(String requestId, String visitorId) {
        activeGenerations.put(requestId, new TourGenerationProgress(requestId, visitorId));
    }

    public void updateProgress(String requestId, double progress, String stage) {
        updateProgress(requestId, progress, stage, null, null);
    }

    public void updateProgress(String requestId, double progress, String stage,
                               Integer currentStopIndex, Integer totalStops) {
        TourGenerationProgress tracking = activeGenerations.get(requestId);
        if (tracking != null) {
            tracking.update(progress, stage, currentStopIndex, totalStops);

            // Only schedule removal if complete
            if ("complete".equals(stage) || tracking.isHasError()) {
                // Keep progress data available for 30 seconds
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS)
                        .execute(() -> activeGenerations.remove(requestId));
            }
        }
    }

    public Optional<TourGenerationProgress> getProgressForDevice(String requestId, String deviceFingerprint) {
        TourGenerationProgress progress = activeGenerations.get(requestId);

        if (progress != null && progress.getVisitorId().equals(deviceFingerprint)) {
            return Optional.of(progress);
        }

        return Optional.empty();
    }
}