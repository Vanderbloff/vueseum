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
    private String artistPrefix;
    private String artistRole;
    private String fullAttribution;
    private boolean isConfidentAttribution;
    private String dimensions;
    private String medium;
    private String culture;
    private String country;
    private String region;
    private String subRegion;
    private String geographyType;
    private String classification;
    private String copyrightStatus;
    private String imageUrl;
    private String description;
    private String galleryNumber;
    private String creationDate;
    private MuseumDTO museum;
    private List<String> tags;

    public static ArtworkDetailsDTO fromEntity(Artwork artwork) {
        ArtworkDetailsDTO dto = new ArtworkDetailsDTO();
        dto.id = artwork.getId();
        dto.externalId = artwork.getExternalId();
        dto.title = artwork.getTitle();
        dto.artistName = artwork.getArtistName();
        dto.artistPrefix = artwork.getArtistPrefix();
        dto.artistRole = artwork.getArtistRole();
        dto.fullAttribution = artwork.getFullAttribution();
        dto.isConfidentAttribution = artwork.isConfidentAttribution();
        dto.medium = artwork.getMedium();
        dto.culture = artwork.getCulture();
        dto.imageUrl = artwork.getImageUrl();
        dto.description = artwork.getDescription();
        dto.country = artwork.getCountry();
        dto.region = artwork.getRegion();
        dto.subRegion = artwork.getSubRegion();
        dto.geographyType = artwork.getGeographyType();
        dto.classification = artwork.getClassification();
        dto.copyrightStatus = artwork.getCopyrightStatus();
        dto.galleryNumber = artwork.getGalleryNumber();
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
        dto.artistName = details.getArtistName();
        dto.artistPrefix = details.getArtistPrefix();
        dto.artistRole = details.getArtistRole();
        dto.fullAttribution = details.getFullAttribution();
        dto.isConfidentAttribution = details.isConfidentAttribution();
        dto.dimensions = details.getDimensions();
        dto.medium = details.getMedium();
        dto.country = details.getCountry();
        dto.region = details.getRegion();
        dto.subRegion = details.getSubRegion();
        dto.geographyType = details.getGeographyType();
        dto.classification = details.getArtworkType();
        dto.copyrightStatus = details.getCopyrightStatus();
        dto.culture = details.getCulture();
        dto.imageUrl = details.getPrimaryImageUrl();
        dto.description = details.getDescription();
        dto.galleryNumber = details.getGalleryNumber();
        dto.creationDate = details.getCreationYear();
        dto.tags = new ArrayList<>(details.getTags());
        return dto;
    }
}
