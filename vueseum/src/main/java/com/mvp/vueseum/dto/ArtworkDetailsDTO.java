package com.mvp.vueseum.dto;

import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Artwork;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ArtworkDetailsDTO {
    private Long id;
    private String externalId;
    private String title;
    private String artistName;
    private String medium;
    private String culture;
    private String imageUrl;
    private String description;
    private String currentLocation;
    private String galleryNumber;
    private Boolean isOnDisplay;
    private String creationDate;
    private MuseumDTO museum;
    private List<String> tags;

    public static ArtworkDetailsDTO fromEntity(Artwork artwork) {
        ArtworkDetailsDTO dto = new ArtworkDetailsDTO();
        dto.id = artwork.getId();
        dto.externalId = artwork.getExternalId();
        dto.title = artwork.getTitle();
        dto.artistName = artwork.getArtistNameAndAttribution();  // Using existing helper method
        dto.medium = artwork.getMedium();
        dto.culture = artwork.getCulture();
        dto.imageUrl = artwork.getImageUrl();
        dto.description = artwork.getDescription();
        dto.currentLocation = artwork.getCurrentLocation();
        dto.galleryNumber = artwork.getGalleryNumber();
        dto.isOnDisplay = artwork.getIsOnDisplay();
        dto.creationDate = artwork.getCreationDate();
        dto.museum = artwork.getMuseum() != null ?
                MuseumDTO.fromEntity(artwork.getMuseum()) : null;

        @SuppressWarnings("unchecked")
        List<String> tagList = (List<String>) artwork.getAdditionalMetadata()
                .getOrDefault("tags", new ArrayList<>());
        dto.tags = new ArrayList<>(tagList);

        return dto;
    }

    public static ArtworkDetailsDTO fromArtworkDetails(ArtworkDetails details) {
        ArtworkDetailsDTO dto = new ArtworkDetailsDTO();
        dto.externalId = details.getExternalId();
        dto.title = details.getTitle();
        dto.artistName = details.getArtistPrefix() + " " + details.getArtistName();
        dto.medium = details.getMedium();
        dto.culture = details.getCulture();
        dto.imageUrl = details.getPrimaryImageUrl();
        dto.description = details.getDescription();
        dto.currentLocation = details.getCurrentLocation();
        dto.galleryNumber = details.getGalleryNumber();
        dto.isOnDisplay = details.getIsOnView();
        dto.creationDate = details.getCreationYear();
        dto.tags = new ArrayList<>(details.getTags());
        return dto;
    }
}
