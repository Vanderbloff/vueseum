package com.mvp.artplatform.dto;

import com.mvp.artplatform.entity.Tour;
import com.mvp.artplatform.entity.TourStop;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TourDTO {
    private Long id;
    private String name;
    private String description;
    private List<TourStopDTO> stops;
    private MuseumDTO museum;  // Instead of just museumName
    private Tour.TourTheme theme;
    private Integer estimatedDuration;
    private Tour.TourDifficulty difficulty;

    public static TourDTO fromEntity(Tour tour) {
        TourDTO dto = new TourDTO();
        dto.id = tour.getId();
        dto.name = tour.getName();
        dto.description = tour.getDescription();
        dto.museum = MuseumDTO.fromEntity(tour.getMuseum());
        dto.theme = tour.getTheme();
        dto.estimatedDuration = tour.getEstimatedDuration();
        dto.difficulty = tour.getDifficulty();
        // Sort stops by sequence number before converting
        dto.stops = tour.getStops().stream()
                .sorted(Comparator.comparing(TourStop::getSequenceNumber))
                .map(TourStopDTO::fromEntity)
                .collect(Collectors.toList());
        return dto;
    }
}
