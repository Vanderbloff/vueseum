package com.mvp.vueseum.client.museum_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mvp.vueseum.client.BaseMuseumApiClient;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.exception.ApiClientException;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Service
@Getter
@PropertySource("classpath:museum.properties")
public class MetMuseumApiClient extends BaseMuseumApiClient {

    private static final Logger logger = LoggerFactory.getLogger(MetMuseumApiClient.class);
    private final RateLimiter rateLimiter;
    private final ArtworkService artworkService;
    private final Museum metMuseum;

    public MetMuseumApiClient(
            Environment environment,
            MuseumService museumService,
            @Value("${museum.metropolitan.api.baseUrl}") String baseUrl,
            ArtworkService artworkService
    ) {
        super(environment, baseUrl);
        // Get or create Met Museum record once during initialization
        this.metMuseum = museumService.findOrCreateMuseum("Metropolitan Museum of Art");
        this.rateLimiter = RateLimiter.create(Integer.parseInt(environment.getProperty("museum.metropolitan.api.rateLimit", "80")));
        this.artworkService = artworkService;
    }

    @VisibleForTesting  // From com.google.common.annotations
    public void syncArtworksForTesting(List<String> artworkIds) {
        syncStartTime = LocalDateTime.now();
        processedCount.set(0);
        errorCount.set(0);

        try {
            processBatch(artworkIds, rateLimiter, artworkService);
        } catch (Exception e) {
            errorCount.incrementAndGet();
            throw new ApiClientException("Test sync failed", e);
        }
    }

    /**
     * Initiates a full synchronization of the museum's collection.
     * This process extracts all object IDs, processes them in batches,
     * and updates the local database.
     */
    @Override
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void syncArtworks() {
        try {
            syncStartTime = LocalDateTime.now();
            logger.info("Starting Met Museum collection sync at {}", syncStartTime);

            // Extract all object IDs
            List<String> allIds = withRetry(
                    () -> {
                        rateLimiter.acquire();
                        String response = restClient.get()
                                .uri("/objects")
                                .retrieve()
                                .body(String.class);
                        return parseSearchResponse(response, "ObjectIDs");
                    },
                    "fetch object IDs from the Met Museum collection"
            );

            // Process in batches to manage memory and respect rate limits
            int batchSize = Integer.parseInt(
                    environment.getProperty("museum.metropolitan.api.rateLimit", "80")
            );
            List<List<String>> batches = Lists.partition(allIds, batchSize);
            for (List<String> batch : batches) {
                try {
                    processBatch(batch, rateLimiter, artworkService);
                    int currentProcessed = processedCount.addAndGet(batch.size());
                    if (currentProcessed % 1000 == 0) {
                        logProgress(currentProcessed, allIds.size());
                    }
                    logger.debug("Processed batch of {} artworks. Total processed: {}", batch.size(), processedCount);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Failed to process batch", e);
                }

                // Respect rate limiting between batches
                rateLimiter.acquire(batch.size());
            }

            logger.info("Completed collection sync. Processed: {}, Errors: {}, Total time: {} minutes",
                    processedCount.get(),
                    errorCount.get(),
                    ChronoUnit.MINUTES.between(syncStartTime, LocalDateTime.now()));

        } catch (Exception e) {
            logger.error("Failed to complete collection sync", e);
            throw new ApiClientException("Collection sync failed", e);
        }
    }

    @Override
    public ArtworkDetails fetchArtworkById(String id) {
        try {
            rateLimiter.acquire();

            String response = restClient.get()
                    .uri("/objects/{id}", id)
                    .retrieve()
                    .body(String.class);

            return convertToArtworkDetails(response);
        }
        catch (HttpClientErrorException.NotFound e) {
            logger.warn("Artwork with id {} not found", id, e);
            return null;
        }
    }

    @Override
    public ArtworkDetails convertToArtworkDetails(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            // Early return if artwork belongs to The Cloisters
            if (rootNode.path("department").asInt() == 7) {
                logger.debug("Skipping Met Museum Cloisters artwork: {}",
                        rootNode.path("objectID").asText());
                return null;  // This artwork will be skipped during processing
            }

            String galleryNumber = rootNode.path("GalleryNumber").asText();
            Boolean isOnDisplay = !galleryNumber.isBlank();

            return ArtworkDetails.builder()
                    .apiSource("Metropolitan Museum of Art")
                    .externalId(rootNode.path("objectID").asText())
                    .title(rootNode.path("title").asText())

                    // Artist information
                    .artistName(rootNode.path("artistDisplayName").asText())  // Remove duplicate
                    .artistNationality(rootNode.path("artistNationality").asText())
                    .artistBirthYear(rootNode.path("artistBeginDate").asText())
                    .artistDeathYear(rootNode.path("artistEndDate").asText())
                    .artistPrefix(rootNode.path("artistPrefix").asText())
                    .artistRole(rootNode.path("artistRole").asText())

                    // Artwork specifics
                    .medium(rootNode.path("medium").asText())
                    .artworkType(rootNode.path("objectName").asText())
                    .dimensions(rootNode.path("dimensions").asText())

                    // Museum location
                    .department(rootNode.path("department").asText())
                    .isOnView(isOnDisplay)
                    .galleryNumber(galleryNumber)

                    // Geographic details - Met-specific richness
                    .country(rootNode.path("country").asText())
                    .region(rootNode.path("region").asText())
                    .subRegion(rootNode.path("subRegion").asText())
                    .geographyType(rootNode.path("geographyType").asText())

                    // Cultural and contextual information
                    .culture(rootNode.path("culture").asText())
                    .period(rootNode.path("period").asText())

                    // Images and metadata
                    .primaryImageUrl(rootNode.path("primaryImage").asText())
                    .additionalImageUrls(rootNode.findValuesAsText("additionalImageUrls"))
                    .tags(rootNode.findValuesAsText("tags"))
                    .creditLine(rootNode.path("creditLine").asText())

                    // Dates and rights
                    .creationYear(rootNode.path("objectDate").asText())
                    .acquisitionDate(rootNode.path("accessionYear").asText())
                    .copyrightStatus(rootNode.path("rightsAndReproduction").asText())  // Added this
                    .build();
        } catch (JsonProcessingException e) {
            throw new ApiClientException("Failed to parse response from Met Museum API", e);
        }
    }

    @Override
    protected Long getMuseumId() {
        return metMuseum.getId();
    }

    /**
     * Updates the display status of artworks in the database.
     * Runs more frequently than the full sync to keep display status current.
     */
    @Scheduled(cron = "0 0 * * * *") // Run hourly
    public void updateDisplayStatus() {
        logger.info("Starting display status update");
        List<Artwork> artworksToCheck = artworkService.findArtworksNeedingDisplayCheck();

        int updatedCount = 0;
        int errorCount = 0;

        for (Artwork artwork : artworksToCheck) {
            try {
                rateLimiter.acquire();
                ArtworkDetails details = fetchArtworkById(artwork.getExternalId());
                artworkService.updateDisplayStatus(artwork.getId(), details.getIsOnView());
                updatedCount++;
            } catch (Exception e) {
                errorCount++;
                logger.warn("Failed to update display status for artwork ID: {}",
                        artwork.getExternalId(), e);
            }
        }

        logger.info("Completed display status update. Updated: {}, Errors: {}",
                updatedCount, errorCount);
    }
}
