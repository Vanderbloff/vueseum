package com.mvp.vueseum.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.exception.ApiClientException;
import com.mvp.vueseum.exception.RetryException;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.util.RetryUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Base class for museum API clients. Each museum implementation should:
 * 1. Handle standard fields (title, artist, etc.) as required fields
 * 2. Map museum-specific formats to our standard structure
 * 3. Leave Met-specific fields (subRegion, geographyType) empty if not applicable
 * 4. Map rights/copyright information to copyrightStatus in a sensible way for that museum
 */
@SuppressWarnings("UnstableApiUsage")
@Slf4j
@Getter
public abstract class BaseMuseumApiClient implements MuseumApiClient {

    @Getter(AccessLevel.NONE)
    protected final AtomicInteger processedCount = new AtomicInteger(0);
    @Getter(AccessLevel.NONE)
    protected final AtomicInteger errorCount = new AtomicInteger(0);
    @Getter(AccessLevel.NONE)
    protected LocalDateTime syncStartTime;

    protected final Logger logger = LoggerFactory.getLogger(BaseMuseumApiClient.class);
    protected final RetryUtil retryUtil;
    protected final RestClient restClient;
    protected final Environment environment;
    protected final String baseUrl;
    protected final ArtworkService artworkService;

    public BaseMuseumApiClient(
            RetryUtil retryUtil,
            Environment environment,
            String baseUrl,
            ArtworkService artworkService) {
        this.retryUtil = retryUtil;
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.artworkService = artworkService;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void syncDisplayedArtworks() {
        try {
            syncStartTime = LocalDateTime.now();
            logger.info("Starting displayed artworks sync at {}", syncStartTime);

            // Get the current list of displayed artworks
            List<String> displayedIds = getCurrentlyDisplayedArtworkIds();
            logger.info("Found {} displayed artworks", displayedIds.size());

            // Process displayed artworks in batches
            int batchSize = getBatchSize();
            List<List<String>> batches = Lists.partition(displayedIds, batchSize);

            for (List<String> batch : batches) {
                try {
                    processDisplayedBatch(batch);
                    int currentProcessed = processedCount.addAndGet(batch.size());
                    if (currentProcessed % 1000 == 0) {
                        logProgress(currentProcessed, displayedIds.size());
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Failed to process batch", e);
                }

                // Respect rate limiting between batches
                getRateLimiter().acquire(batch.size());
            }

            logger.info("Completed displayed artworks sync. Processed: {}, Errors: {}, Total time: {} minutes",
                    processedCount.get(),
                    errorCount.get(),
                    ChronoUnit.MINUTES.between(syncStartTime, LocalDateTime.now()));

        } catch (Exception e) {
            logger.error("Failed to complete displayed artworks sync", e);
            throw new ApiClientException("Displayed artworks sync failed", e);
        }
    }

    protected void logProgress(int currentProcessed, int totalIds) {
        double percentComplete = (double) currentProcessed / totalIds * 100;
        long minutesElapsed = ChronoUnit.MINUTES.between(syncStartTime, LocalDateTime.now());
        double processRate = currentProcessed / (double) Math.max(1, minutesElapsed);
        long estimatedMinutesRemaining = (long) ((totalIds - currentProcessed) / processRate);

        logger.info("Sync Progress: {}% complete ({}/{} items)",
                String.format("%.2f", percentComplete),
                currentProcessed,
                totalIds);
        logger.info("Time elapsed: {} minutes, Estimated time remaining: {} minutes",
                minutesElapsed,
                estimatedMinutesRemaining);
        logger.info("Current processing rate: {} items/minute",
                String.format("%.2f", processRate));
        logger.info("Errors encountered: {}", errorCount.get());
    }

    protected List<String> parseSearchResponse(String searchResponse, String nameOfIdParameter) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(searchResponse);
            JsonNode objectIds = rootNode.get(nameOfIdParameter);

            if (objectIds == null || objectIds.isNull())
                return Collections.emptyList();

            List<String> parsedIds = new ArrayList<>();
            objectIds.forEach(id -> parsedIds.add(id.asText()));
            return parsedIds;

        } catch (JsonProcessingException e) {
            throw new ApiClientException("Failed to parse search response", e);
        }
    }

    /**
     * Processes a batch of artwork IDs, extracting details and saving to the database.
     * Continues processing even if individual artworks fail.
     */
    protected void processDisplayedBatch(List<String> objectIds) {
        for (String id : objectIds) {
            try {
                ArtworkDetails details = fetchArtworkById(id);

                if (details == null) {
                    // Treat null details as an error condition
                    logger.debug("No valid details found for artwork {}, skipping", id);
                    errorCount.incrementAndGet();
                    processedCount.incrementAndGet();
                    continue;
                }

                artworkService.saveFromDetails(details);
                processedCount.incrementAndGet();

            } catch (Exception e) {
                logger.warn("Failed to process artwork ID: {}", id, e);
                // Record the error but continue processing the batch
                artworkService.recordProcessingError(id, getMuseumId(), e);
                errorCount.incrementAndGet();
                processedCount.incrementAndGet();
            }
        }
    }

    protected <T> T withRetry(Supplier<T> operation, String operationName) {
        try {
            return retryUtil.withRetry(operation, operationName, 3);
        } catch (RetryException e) {
            throw new ApiClientException("API operation failed after retries", e);
        }
    }

    @Override
    public int getErrorCount() {
        return errorCount.get();
    }

    @Override
    public LocalDateTime getSyncStartTime() {
        return syncStartTime;
    }

    @Override
    public int getProcessedCount() {
        return processedCount.get();
    }

    // Helper method for processing artworks
    protected void processArtworksSync(List<String> artworkIds) {
        int batchSize = getBatchSize();
        List<List<String>> batches = Lists.partition(artworkIds, batchSize);

        for (List<String> batch : batches) {
            try {
                processDisplayedBatch(batch);
                logProgress(processedCount.get(), artworkIds.size());
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.error("Failed to process batch", e);
            }
            getRateLimiter().acquire(batch.size());
        }
    }

    protected abstract List<String> getUpdatedArtworkIds(LocalDateTime since);
    protected abstract ArtworkDetails convertToArtworkDetails(String apiResponse);
    protected abstract int getBatchSize();
    protected abstract RateLimiter getRateLimiter();
    public abstract List<String> getCurrentlyDisplayedArtworkIds();
    public abstract Long getMuseumId();
}
