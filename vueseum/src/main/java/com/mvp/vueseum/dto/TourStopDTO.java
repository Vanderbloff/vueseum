package com.mvp.vueseum.dto;

import com.mvp.vueseum.entity.TourStop;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TourStopDTO {
    private Long id;
    private Integer sequenceNumber;
    private ArtworkSummaryDTO artwork;  // A simplified view of artwork
    private String description;
    private String standardDescription;  // Adding the standard artwork description
    private Boolean isRequired;

    public static TourStopDTO fromEntity(TourStop stop) {
        TourStopDTO dto = new TourStopDTO();
        dto.id = stop.getId();
        dto.sequenceNumber = stop.getSequenceNumber();
        dto.artwork = ArtworkSummaryDTO.fromEntity(stop.getArtwork());
        dto.description = stop.getTourContextDescription();
        dto.standardDescription = stop.getStandardDescription();
        dto.isRequired = stop.isRequired();
        return dto;
    }
}
