package com.mvp.vueseum.dto;

import com.mvp.vueseum.domain.ArtworkDetails;
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
