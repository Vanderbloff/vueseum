package com.mvp.artplatform.client.museum_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.mvp.artplatform.dto.ArtworkDetails;
import com.mvp.artplatform.client.BaseMuseumApiClient;
import com.mvp.artplatform.exception.ApiClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetMuseumApiClient extends BaseMuseumApiClient {

    private RateLimiter rateLimiter;
    public MetMuseumApiClient(
            Environment environment,
            @Value("${museum.api.metmuseum.baseUrl}") String baseUrl
    ) {
        super(environment, baseUrl);
        this.rateLimiter = RateLimiter.create(Integer.parseInt(environment.getProperty("museum.metropolitan.api.rateLimit", "80")));
    }

    @Override
    public ArtworkDetails fetchArtworkById(String id) {
        rateLimiter.acquire();

        // RestClient provides a more straightforward way to make HTTP requests
        String response = restClient.get()
                .uri("/objects/{id}", id)
                .retrieve()
                .body(String.class);

        System.out.println(response);
        return convertToArtworkDetails(response);
    }

    @Override
    public ArtworkDetails convertToArtworkDetails(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            String galleryNumber = rootNode.get("GalleryNumber").asText();
            Boolean isOnDisplay = galleryNumber.isBlank();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");


            return ArtworkDetails.builder()
                    .apiSource("Metropolitan Museum of Art")
                    .externalId(rootNode.get("objectID").asText())
                    .title(rootNode.get("title").asText())

                    .artistName(rootNode.get("artistDisplayName").asText())
                    .artistNationality(rootNode.get("artistNationality").asText())
                    .artistBirthYear(rootNode.get("artistBeginDate").asText())
                    //.artistBirthYear(objectMapper.convertValue(rootNode.get("artistBeginDate"), LocalDate.class))
                    .artistDeathYear(rootNode.get("artistEndDate").asText())
                    //.artistDeathYear(objectMapper.convertValue(rootNode.get("artistEndDate"), LocalDate.class))

                    .medium(rootNode.get("medium").asText())
                    .artworkType(rootNode.get("objectName").asText())
                    .dimensions(rootNode.get("dimensions").asText())

                    .department(rootNode.get("department").asText())
                    .isOnView(isOnDisplay)
                    .currentLocation(rootNode.get("country").asText())
                    .galleryNumber(galleryNumber)

                    .tags(rootNode.findValuesAsText("tags"))
                    .creditLine(rootNode.get("creditLine").asText())

                    .culture(rootNode.get("culture").asText())
                    .period(rootNode.get("period").asText())

                    .primaryImageUrl(rootNode.get("primaryImage").asText())
                    .additionalImageUrls(rootNode.findValuesAsText("additionalImageUrls"))

                    .creationYear(rootNode.get("objectDate").asText())
                    //.creationYear(objectMapper.convertValue(rootNode.get("objectDate"), LocalDate.class))
                    .acquisitionDate(rootNode.get("accessionYear").asText())
                    //.acquisitionDate(objectMapper.convertValue(rootNode.get("acquisitionDate"), LocalDate.class))
                    .build();
        } catch (JsonProcessingException e) {
            throw new ApiClientException("Failed to parse response from Met Museum API", e);
        }
    }

    @Override
    public List<ArtworkDetails> searchArtworks(Map<String, String> criteria) {
        String searchResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search?q")
                        .queryParams(convertToMultiValueMap(criteria))
                        .build())
                .retrieve()
                .body(String.class);

        List<String> objectIds = parseSearchResponse(searchResponse);

        return objectIds.stream()
                .limit(100)
                .map(this::fetchArtworkById)
                .collect(Collectors.toList());
    }

    private List<String> parseSearchResponse(String searchResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(searchResponse);
            JsonNode objectIds = rootNode.get("objectIDs");

            if (objectIds == null || objectIds.isNull())
                return Collections.emptyList();

            List<String> parsedIds = new ArrayList<>();
            objectIds.forEach(id -> parsedIds.add(id.asText()));
            return parsedIds;

        } catch (JsonProcessingException e) {
            throw new ApiClientException("Failed to parse search response", e);
        }
    }

    private MultiValueMap<String, String> convertToMultiValueMap(Map<String, String> criteria) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }
        return params;
    }
}
