package com.mvp.vueseum.dto;

import com.mvp.vueseum.entity.Artwork;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArtworkSummaryDTO {
    private Long id;
    private String title;
    private String artistName;
    private String artistPrefix;
    private String artistRole;
    private String fullAttribution;
    private String medium;
    private String classification;
    private String culture;
    private String country;
    private String imageUrl;
    private String secondaryImageUrl;
    private String galleryNumber;
    private String department;

    public static ArtworkSummaryDTO fromEntity(Artwork artwork) {
        ArtworkSummaryDTO dto = new ArtworkSummaryDTO();
        dto.id = artwork.getId();
        dto.title = artwork.getTitle();
        dto.artistName = artwork.getArtistName();
        dto.artistPrefix = artwork.getArtistPrefix();
        dto.artistRole = artwork.getArtistRole();
        dto.fullAttribution = artwork.getFullAttribution();
        dto.medium = artwork.getMedium();
        dto.classification = artwork.getClassification();
        dto.culture = artwork.getCulture();
        dto.country = artwork.getCountry();
        dto.imageUrl = artwork.getImageUrl();
        dto.secondaryImageUrl = artwork.getThumbnailImageUrl();
        dto.galleryNumber = artwork.getGalleryNumber();
        dto.department = artwork.getDepartment();
        return dto;
    }
}