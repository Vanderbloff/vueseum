package com.mvp.artplatform.client.museum_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.artplatform.client.BaseMuseumApiClient;
import com.mvp.artplatform.model.Artist;
import com.mvp.artplatform.model.Artwork;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MetMuseumApiClient extends BaseMuseumApiClient {

    public MetMuseumApiClient(String baseUrl) {
        super(baseUrl);
    }

    @Override
    public List<Artwork> fetchArtworks() {
        String endpoint = baseUrl + "/objects";
        ResponseEntity<String> initialResponse = restClient.get()
                .uri(endpoint)
                .retrieve()
                .toEntity(String.class);

        List<Integer> objectIds = parseObjectIds(initialResponse.getBody());

        return objectIds.stream()
                .map(this::fetchArtworkDetails)
                .collect(Collectors.toList());
    }

    private List<Integer> parseObjectIds(String body) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(body);
            return mapper.convertValue(
                    rootNode.get("objectIds"),
                    new TypeReference<>() {
                    }
            );
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Artwork> fetchArtworksByArtist(String artistName) {
        return List.of();
    }

    @Override
    public Artwork fetchArtworkDetails(Integer artworkId) {

        String endpoint = baseUrl + "/objects/" + artworkId;
        ResponseEntity<String> detailResponse = restClient.get()
                .uri(endpoint)
                .retrieve()
                .toEntity(String.class);

        // Convert raw JSON directly to ArtworkDetails
        return convertToArtwork(detailResponse.getBody());
    }

    @Override
    public Artwork convertToArtwork(String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(body);
            Artist artist = new Artist();
            artist.setArtistName(rootNode.get("artistDisplayName").asText());
            artist.setNationality(rootNode.get("artistNationality").asText());
            artist.setBirthDate(LocalDate.parse(rootNode.get("artistBeginDate").asText()));
            artist.setDeathDate(LocalDate.parse(rootNode.get("artistEndDate").asText()));

            Artwork artwork = new Artwork();
            artwork.setArtist(artist);
            artwork.setTitle(rootNode.get("title").asText());
            artwork.setMedium(rootNode.get("medium").asText());
            artwork.setImageUrl(rootNode.get("primaryImage").asText());
            artwork.setCountry(rootNode.get("country").asText());
            artwork.setGalleryNumber(rootNode.get("GalleryNumber").asText());
            artwork.setCreationDate(rootNode.get("objectDate").asText());

            return artwork;
        } catch (JsonProcessingException e) {
            return new Artwork();
        }
    }

    /*@Override
    protected List<Artwork> transformResponseToArtworks(MetMuseumResponse apiResponse) {
        return apiResponse.getObjectIds().stream()
                .map()
                .collect(Collectors.toList());
    }*/
}
