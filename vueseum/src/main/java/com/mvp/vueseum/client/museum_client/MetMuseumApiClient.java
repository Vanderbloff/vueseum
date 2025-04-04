package com.mvp.vueseum.client.museum_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.mvp.vueseum.client.BaseMuseumApiClient;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.exception.ApiClientException;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.util.RetryUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Service
@Getter
@Slf4j
@PropertySource("classpath:museum.properties")
public class MetMuseumApiClient extends BaseMuseumApiClient {
    private final MuseumService museumService;
    private Museum metMuseum;

    @Getter(AccessLevel.NONE)
    private final RateLimiter rateLimiter;

    //private static int page = 1;

    public MetMuseumApiClient(
            RetryUtil retryUtil,
            Environment environment,
            MuseumService museumService,
            @Value("${museum.metropolitan.api.baseUrl}") String baseUrl,
            ArtworkService artworkService
    ) {
        super(retryUtil, environment, baseUrl, artworkService);
        this.museumService = museumService;
        this.rateLimiter = RateLimiter.create(
                Integer.parseInt(
                        environment.getProperty("museum.metropolitan.api.rateLimit", "80")
                )
        );
    }

    @Override
    public Long getMuseumId() {
        if (metMuseum == null) {
            metMuseum = museumService.findOrCreateMuseum("Metropolitan Museum of Art");
        }
        return metMuseum.getId();
    }

    @Override
    protected int getBatchSize() {
        return Integer.parseInt(environment.getProperty("museum.metropolitan.api.batchSize", "80"));
    }

    @Override
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public boolean isArtworkDisplayed(String response) {
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
            log.warn("Failed to parse artwork response", e);
            return false;
        }
    }

    @Override
    public List<String> getCurrentlyDisplayedArtworkIds() {
        return withRetry(() -> {
            // Use the Met Museum's recommended format, with parameters in this specific order
            String initialUrl = baseUrl + "/search?isOnView=true&q=*";
            log.info("Requesting Met API URL: {}", initialUrl);

            rateLimiter.acquire();
            String initialResponse = restClient.get()
                    .uri(initialUrl)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;
            try {
                rootNode = mapper.readTree(initialResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            int totalArtworks = rootNode.path("total").asInt();
            log.info("Met API reports {} total artworks on display", totalArtworks);

            List<String> allIds = parseSearchResponse(initialResponse, "objectIDs");
            log.info("Retrieved {} artwork IDs", allIds.size());

            return allIds;
        }, "fetch displayed artwork IDs");
    }

    @Override
    protected List<String> getUpdatedArtworkIds(LocalDateTime since) {
        return withRetry(() -> {
            rateLimiter.acquire();

            String formattedDate = since.format(DateTimeFormatter.ISO_DATE);

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/objects")
                            .queryParam("metadataDate", formattedDate)
                            .build())
                    .retrieve()
                    .body(String.class);

            List<String> updatedIds = parseSearchResponse(response, "objectIDs");

            // Filter for displayed artworks
            return updatedIds.stream()
                    .filter(id -> isArtworkDisplayed(fetchObjectResponse(id)))
                    .collect(Collectors.toList());
        }, "fetch updated artwork IDs");
    }

    @Override
    public ArtworkDetails fetchArtworkById(String id) {
        try {
            String response = fetchObjectResponse(id);
            return convertToArtworkDetails(response);
        }
        catch (HttpClientErrorException.NotFound e) {
            log.warn("Artwork with id {} not found", id, e);
            return null;
        }
    }

    private String fetchObjectResponse(String id) {
        rateLimiter.acquire();
        return restClient.get()
                .uri("/objects/{id}", id)
                .retrieve()
                .body(String.class);
    }

    private boolean validateImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Void> response = restClient.get()
                    .uri(url)
                    .header("Accept", "image/*")
                    .retrieve()
                    .toBodilessEntity();

            // Check both status and content type
            return response.getStatusCode().is2xxSuccessful() &&
                    response.getHeaders().getContentType() != null &&
                    !response.getHeaders().getContentType().includes(MediaType.TEXT_HTML);
        } catch (Exception e) {
            log.warn("Image URL validation failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    @Override
    public ArtworkDetails convertToArtworkDetails(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(response);
            if (rootNode.path("department").asInt() == 7 ||
                "The Cloisters".equals(rootNode.path("department").asText()) ||
                    (rootNode.has("location") &&
                            rootNode.path("location").asText().contains("Cloisters"))) {
                log.debug("Skipping Cloisters artwork: {}",
                        rootNode.path("objectID").asText());
                return null;
            }
        } catch (IOException e) {
            throw new ApiClientException("Invalid JSON response from Met Museum API", e);
        }

        // Validate required fields
        if (rootNode.path("objectID").isMissingNode() ||
                !rootNode.has("objectID") ||
                rootNode.path("objectID").asText().isEmpty()) {
            throw new ApiClientException("Missing required field: objectID");
        }

        // Check if artwork is displayed (early return)
        if (!isArtworkDisplayed(response)) {
            return null;
        }

        try {
            // Extract image URLs with detailed logging
            String primaryImageUrl = rootNode.path("primaryImage").asText("");
            String thumbnailImageUrl = rootNode.path("primaryImageSmall").asText("");


            if (!primaryImageUrl.isEmpty() && !validateImageUrl(primaryImageUrl)) {
                log.info("Primary image URL invalid, falling back to thumbnail: {}", primaryImageUrl);
                primaryImageUrl = thumbnailImageUrl;
            }

            if (!thumbnailImageUrl.isEmpty() && !validateImageUrl(thumbnailImageUrl)) {
                thumbnailImageUrl = "";
            }

            // Process artist dates
            String birthYear = rootNode.path("artistBeginDate").asText("");
            String deathYear = rootNode.path("artistEndDate").asText("");

            // Only use years that are exactly 4 digits
            birthYear = birthYear.matches("^[0-9]{4}$") ? birthYear : "";
            deathYear = deathYear.matches("^[0-9]{4}$") ? deathYear : "";

            return ArtworkDetails.builder()
                    .apiSource("Metropolitan Museum of Art")
                    .externalId(rootNode.path("objectID").asText())
                    .title(rootNode.path("title").asText())
                    .artistName(rootNode.path("artistDisplayName").asText())
                    .artistNationality(rootNode.path("artistNationality").asText())
                    .artistBirthYear(birthYear)
                    .artistDeathYear(deathYear)
                    .artistPrefix(rootNode.path("artistPrefix").asText())
                    .artistRole(rootNode.path("artistRole").asText())
                    .medium(rootNode.path("medium").asText())
                    .artworkType(rootNode.path("objectName").asText())
                    .creationYear(rootNode.path("objectDate").asText())
                    .dimensions(rootNode.path("dimensions").asText())
                    .department(rootNode.path("department").asText())
                    .galleryNumber(rootNode.path("GalleryNumber").asText())
                    .country(rootNode.path("country").asText())
                    .region(rootNode.path("region").asText())
                    .subRegion(rootNode.path("subRegion").asText())
                    .geographyType(rootNode.path("geographyType").asText())
                    .culture(rootNode.path("culture").asText())
                    .period(rootNode.path("period").asText())
                    .primaryImageUrl(primaryImageUrl)
                    .thumbnailImageUrl(thumbnailImageUrl)
                    .build();

        } catch (Exception e) {
            throw new ApiClientException("Failed to parse response from Met Museum API", e);
        }
    }
}
