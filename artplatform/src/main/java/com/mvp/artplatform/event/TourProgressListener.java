package com.mvp.artplatform.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TourProgressListener {

    @EventListener
    public void handleTourProgress(TourGenerationProgressEvent event) {
        // Log progress for monitoring
        log.info("Tour Generation Progress: {}", event.getFormattedStatus());

        // Here you could also:
        // - Send progress to connected WebSocket clients
        // - Update a progress tracker in a database
        // - Trigger notifications if generation is taking too long
    }
}
