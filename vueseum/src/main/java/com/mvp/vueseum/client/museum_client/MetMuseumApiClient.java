package com.mvp.vueseum.client.museum_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.mvp.vueseum.client.BaseMuseumApiClient;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.event.SyncOperation;
import com.mvp.vueseum.exception.ApiClientException;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.util.RetryUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Service
@Getter
@PropertySource("classpath:museum.properties")
public class MetMuseumApiClient extends BaseMuseumApiClient {
    private static final Logger logger = LoggerFactory.getLogger(MetMuseumApiClient.class);
    private final Museum metMuseum;

    @Getter(AccessLevel.NONE)
    private final RateLimiter rateLimiter;

    public MetMuseumApiClient(
            RetryUtil retryUtil,
            Environment environment,
            MuseumService museumService,
            @Value("${museum.metropolitan.api.baseUrl}") String baseUrl,
            ArtworkService artworkService
    ) {
        super(retryUtil, environment, baseUrl, artworkService);
        this.metMuseum = museumService.findOrCreateMuseum("Metropolitan Museum of Art");
        this.rateLimiter = RateLimiter.create(
                Integer.parseInt(
                        environment.getProperty("museum.metropolitan.api.rateLimit", "80")
                )
        );
    }

    @Override
    public List<String> getCurrentlyDisplayedArtworkIds() {
        return withRetry(() -> {
            rateLimiter.acquire();
            String response = restClient.get()
                    .uri("/objects")
                    .retrieve()
                    .body(String.class);

            List<String> allIds = parseSearchResponse(response, "ObjectIDs");

            return allIds.stream()
                    .filter(id -> {
                        rateLimiter.acquire();
                        String detailResponse = restClient.get()
                                .uri("/objects/{id}", id)
                                .retrieve()
                                .body(String.class);
                        return isArtworkDisplayed(detailResponse);
                    })
                    .collect(Collectors.toList());
        }, "fetch displayed artwork IDs");
    }

    private boolean isArtworkDisplayed(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            // Skip Cloisters
            if (rootNode.path("department").asInt() == 7) {
                return false;
            }

            // Check if artwork has a gallery number
            return !rootNode.path("GalleryNumber").asText().isBlank();
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse artwork response", e);
            return false;
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
            if (!isArtworkDisplayed(response)) {
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

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
                    .galleryNumber(rootNode.path("GalleryNumber").asText())

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
    protected List<String> getUpdatedArtworkIds(LocalDateTime since) {
        return withRetry(() -> {
            rateLimiter.acquire();

            // Format date as required by Met API (YYYY-MM-DD)
            String formattedDate = since.format(DateTimeFormatter.ISO_DATE);

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/objects")
                            .queryParam("metadataDate", formattedDate)
                            .build())
                    .retrieve()
                    .body(String.class);

            List<String> updatedIds = parseSearchResponse(response, "ObjectIDs");

            // Filter for displayed artworks
            return updatedIds.stream()
                    .filter(id -> isArtworkDisplayed(fetchObjectResponse(id)))
                    .collect(Collectors.toList());
        }, "fetch updated artwork IDs");
    }

    // Helper method to get object response
    private String fetchObjectResponse(String id) {
        rateLimiter.acquire();
        return restClient.get()
                .uri("/objects/{id}", id)
                .retrieve()
                .body(String.class);
    }

    @Override
    public void performSync(SyncOperation operation) {
        try {
            syncStartTime = operation.getStartTime();
            logger.info("Starting {} sync at {}",
                    operation.isFullSync() ? "full" : "incremental",
                    syncStartTime);

            List<String> artworkIds = operation.isFullSync()
                    ? getCurrentlyDisplayedArtworkIds()
                    : getUpdatedArtworkIds(operation.getIncrementalSince());

            processArtworksSync(artworkIds);

            logger.info("Completed {} sync. Processed: {}, Errors: {}",
                    operation.isFullSync() ? "full" : "incremental",
                    processedCount.get(),
                    errorCount.get());

        } catch (Exception e) {
            logger.error("Failed to complete sync", e);
            throw new ApiClientException("Sync failed", e);
        }
    }

    @Override
    public Long getMuseumId() {
        return metMuseum.getId();
    }

    @Override
    protected int getBatchSize() {
        return Integer.parseInt(
                environment.getProperty("museum.metropolitan.api.batchSize", "80")
        );
    }

    @Override
    protected RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}
