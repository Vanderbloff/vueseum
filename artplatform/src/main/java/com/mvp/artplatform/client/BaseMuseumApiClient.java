package com.mvp.artplatform.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.mvp.artplatform.domain.ArtworkDetails;
import com.mvp.artplatform.exception.ApiClientException;
import com.mvp.artplatform.exception.ResourceNotFoundException;
import com.mvp.artplatform.service.artwork.ArtworkService;
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

@Slf4j
@Getter
public abstract class BaseMuseumApiClient implements MuseumApiClient {

    protected final RestClient restClient;
    protected final Environment environment;
    protected final String baseUrl;
    protected final AtomicInteger processedCount = new AtomicInteger(0);
    protected final AtomicInteger errorCount = new AtomicInteger(0);
    protected LocalDateTime syncStartTime;
    protected final Logger logger = LoggerFactory.getLogger(LoggerFactory.getLogger(getClass()).getClass());

    public BaseMuseumApiClient(Environment environment, String baseUrl) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    protected abstract ArtworkDetails convertToArtworkDetails(String apiResponse);

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
    protected void processBatch(List<String> objectIds, RateLimiter rateLimiter, ArtworkService artworkService) {
        for (String id : objectIds) {
            try {
                // Skip if we already have a recent version
                if (artworkService.isRecentlyUpdated(id)) {
                    logger.debug("Skipping recently updated artwork: {}", id);
                    processedCount.incrementAndGet();
                    continue;
                }

                rateLimiter.acquire();
                ArtworkDetails details = fetchArtworkById(id);

                if (details == null) {
                    // Treat null details as an error condition
                    logger.warn("No artwork details found for ID: {}", id);
                    artworkService.recordProcessingError(id, new ResourceNotFoundException("Artwork not found"));
                    errorCount.incrementAndGet();
                    processedCount.incrementAndGet();
                    continue;
                }

                artworkService.saveFromDetails(details);
                processedCount.incrementAndGet();

            } catch (Exception e) {
                logger.warn("Failed to process artwork ID: {}", id, e);
                // Record the error but continue processing the batch
                artworkService.recordProcessingError(id, e);
                errorCount.incrementAndGet();
                processedCount.incrementAndGet();
            }
        }
    }

    protected <T> T withRetry(Supplier<T> operation, String operationName) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                retryCount++;
                logger.warn("Failed {} (attempt {}/{})", operationName, retryCount, maxRetries, e);

                if (retryCount == maxRetries) {
                    throw new ApiClientException("Failed " + operationName + " after " + maxRetries + " attempts", e);
                }

                try {
                    Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiClientException("Interrupted while waiting to retry", ie);
                }
            }
        }
        throw new ApiClientException("Failed " + operationName + " after exhausting retries");
    }
}
