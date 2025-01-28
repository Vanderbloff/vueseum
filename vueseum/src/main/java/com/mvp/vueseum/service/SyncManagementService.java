package com.mvp.vueseum.service;

import com.mvp.vueseum.client.MuseumApiClient;
import com.mvp.vueseum.event.SyncOperation;
import com.mvp.vueseum.exception.PersistenceException;
import com.mvp.vueseum.exception.ResourceNotFoundException;
import com.mvp.vueseum.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncManagementService {
    private final List<MuseumApiClient> museumApiClients;
    private final ArtworkRepository artworkRepository;
    protected final Logger logger = LoggerFactory.getLogger(SyncManagementService.class);

    @Scheduled(cron = "0 0 1 * * *")  // Run at 1 AM daily
    public void scheduledSync() {
        // On the first day of the month, do a full sync
        LocalDateTime now = LocalDateTime.now();
        SyncOperation operation = now.getDayOfMonth() == 1
                ? SyncOperation.monthly()
                : SyncOperation.daily();

        logger.info("Starting scheduled {} sync at {}",
                operation.isFullSync() ? "full" : "incremental",
                now);

        try {
            startSync(null, operation);
            logger.info("Completed scheduled sync successfully");
        } catch (Exception e) {
            logger.error("Scheduled sync failed", e);
        }
    }

    public void startSync(Long museumId, SyncOperation operation) {
        if (museumId != null) {
            MuseumApiClient client = findClientForMuseum(museumId);
            processSync(client, operation);
        } else {
            // Process each museum independently
            museumApiClients.forEach(client -> {
                try {
                    processSync(client, operation);
                } catch (Exception e) {
                    logger.error("Sync failed for museum {}", client.getMuseumId(), e);
                    // Continue with next museum instead of rethrowing
                }
            });
        }
    }

    private void processSync(MuseumApiClient client, SyncOperation operation) {
        try {
            client.performSync(operation);
        } catch (PersistenceException e) {
            logger.error("Sync failed for museum {}", client.getMuseumId(), e);
            throw e;
        }
    }

    public Map<String, Object> getAggregatedStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalDisplayedArtworks", artworkRepository.count());

        List<Map<String, Object>> museumStatuses = museumApiClients.stream()
                .map(this::getClientStatus)
                .collect(Collectors.toList());

        status.put("museumStatuses", museumStatuses);
        return status;
    }

    public Map<String, Object> getSingleMuseumStatus(Long museumId) {
        return getClientStatus(findClientForMuseum(museumId));
    }

    private Map<String, Object> getClientStatus(MuseumApiClient client) {
        return Map.of(
                "museumId", client.getMuseumId(),
                "artworkCount", artworkRepository.countByMuseum(client.getMuseumId()),
                "processingErrors", client.getErrorCount(),
                "syncStartTime", client.getSyncStartTime(),
                "processedCount", client.getProcessedCount()
        );
    }

    private MuseumApiClient findClientForMuseum(Long museumId) {
        return museumApiClients.stream()
                .filter(client -> client.getMuseumId().equals(museumId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No API client found for museum ID: " + museumId));
    }
}
