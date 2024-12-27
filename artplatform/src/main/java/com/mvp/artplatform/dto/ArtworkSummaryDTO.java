package com.mvp.artplatform.dto;

import com.mvp.artplatform.entity.Artwork;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArtworkSummaryDTO {
    private Long id;
    private String title;
    private String artistName;
    private String medium;
    private String imageUrl;
    private String currentLocation;
    private Boolean isOnDisplay;

    // A simplified artwork view for tour stops
    public static ArtworkSummaryDTO fromEntity(Artwork artwork) {
        ArtworkSummaryDTO dto = new ArtworkSummaryDTO();
        dto.id = artwork.getId();
        dto.title = artwork.getTitle();
        dto.artistName = artwork.getArtistName();  // Using existing helper method
        dto.medium = artwork.getMedium();
        dto.imageUrl = artwork.getImageUrl();
        dto.currentLocation = artwork.getCurrentLocation();
        dto.isOnDisplay = artwork.getIsOnDisplay();
        return dto;
    }
}
