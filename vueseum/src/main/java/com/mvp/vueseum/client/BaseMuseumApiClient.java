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

    /**
     * Processes a batch of artwork IDs, extracting details and saving to the database.
     * Continues processing even if individual artworks fail.
     */
    protected void processDisplayedBatch(List<String> objectIds) {
        List<List<String>> batches = Lists.partition(objectIds, getBatchSize());
        List<String> failedIds = new ArrayList<>();
        int batchCount = 0;
        int totalBatches = batches.size();

        for (List<String> batch : batches) {
            batchCount++;
            int batchSuccessCount = 0;
            List<String> batchFailedIds = new ArrayList<>();

            try {
                for (String id : batch) {
                    try {
                        ArtworkDetails details = fetchArtworkById(id);
                        if (details == null) {
                            log.debug("No valid details found for artwork {}, skipping", id);
                            errorCount.incrementAndGet();
                            processedCount.incrementAndGet();
                            batchFailedIds.add(id);
                            continue;
                        }

                        if (details.getMedium() != null && details.getMedium().length() > 1000) {
                            details.setMedium(details.getMedium().substring(0, 997) + "...");
                            log.warn("Truncated medium field for artwork {}", details.getExternalId());
                        }

                        artworkService.saveFromDetails(details);
                        processedCount.incrementAndGet();
                        batchSuccessCount++;

                    } catch (Exception e) {
                        log.warn("Failed to process artwork ID: {}", id, e);
                        artworkService.recordProcessingError(id, getMuseumId(), e);
                        errorCount.incrementAndGet();
                        processedCount.incrementAndGet();
                        batchFailedIds.add(id);
                    }
                }

                logProgress(processedCount.get(), objectIds.size());
                log.info("Batch {}/{} completed: {} succeeded, {} failed",
                        batchCount, totalBatches, batchSuccessCount, batchFailedIds.size());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("Failed to process batch {}/{}", batchCount, totalBatches, e);
                batchFailedIds.addAll(batch.stream()
                        .filter(id -> !batchFailedIds.contains(id))
                        .toList());
            }

            if (!batchFailedIds.isEmpty()) {
                failedIds.addAll(batchFailedIds);
            }

            getRateLimiter().acquire(batch.size());
        }

        log.info("Completed processing. Processed: {}, Errors: {}, Total time: {} minutes",
                processedCount.get(),
                errorCount.get(),
                ChronoUnit.MINUTES.between(syncStartTime, LocalDateTime.now()));

        if (!failedIds.isEmpty() && failedIds.size() < Math.max(100, objectIds.size() * 0.05)) {
            // Only retry if failures are < 5% of total or < 100 artworks (whichever is larger)
            log.info("Attempting to retry {} failed artworks", failedIds.size());
            int retriedSuccessfully = retryFailedArtworks(failedIds);
            if (retriedSuccessfully > 0) {
                log.info("Successfully retried {} out of {} failed artworks",
                        retriedSuccessfully, failedIds.size());
            }
        }
    }

    private int retryFailedArtworks(List<String> failedIds) {
        int retrySuccess = 0;

        for (String id : failedIds) {
            try {
                // Add a small delay between retries
                Thread.sleep(100L);

                log.info("Retrying artwork ID: {}", id);
                ArtworkDetails details = fetchArtworkById(id);

                if (details == null) {
                    log.debug("Retry: No valid details for artwork {}", id);
                    continue;
                }

                if (details.getMedium() != null && details.getMedium().length() > 1000) {
                    details.setMedium(details.getMedium().substring(0, 997) + "...");
                }

                artworkService.saveFromDetails(details);
                log.info("Successfully retried artwork ID: {}", id);
                errorCount.decrementAndGet();
                retrySuccess++;

            } catch (Exception e) {
                log.warn("Retry failed for artwork ID: {}", id, e);
            }

            getRateLimiter().acquire();
        }

        return retrySuccess;
    }

    protected void logProgress(int currentProcessed, int totalIds) {
        if (currentProcessed % 1000 != 0) return;

        double percentComplete = (double) currentProcessed / totalIds * 100;
        long minutesElapsed = ChronoUnit.MINUTES.between(syncStartTime, LocalDateTime.now());
        double processRate = currentProcessed / (double) Math.max(1, minutesElapsed);
        long estimatedMinutesRemaining = (long) ((totalIds - currentProcessed) / processRate);

        log.info("Sync Progress: {}% complete ({}/{} items)",
                String.format("%.2f", percentComplete),
                currentProcessed,
                totalIds);
        log.info("Time elapsed: {} minutes, Estimated time remaining: {} minutes",
                minutesElapsed,
                estimatedMinutesRemaining);
        log.info("Current processing rate: {} items/minute",
                String.format("%.2f", processRate));
        log.info("Errors encountered: {}", errorCount.get());
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

    protected abstract boolean isArtworkDisplayed(String apiResponse);
    protected abstract List<String> getUpdatedArtworkIds(LocalDateTime since);
    protected abstract ArtworkDetails convertToArtworkDetails(String apiResponse);
    protected abstract int getBatchSize();
    protected abstract RateLimiter getRateLimiter();
    public abstract List<String> getCurrentlyDisplayedArtworkIds();
    public abstract Long getMuseumId();
}
