package com.mvp.vueseum.dto;

import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.TourStop;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TourDTO {
    private Long id;
    private String deviceFingerprint;
    private String name;
    private String description;
    private List<TourStopDTO> stops;
    private MuseumDTO museum;
    private Tour.TourTheme theme;
    private Tour.TourDifficulty difficulty;

    public static TourDTO fromEntity(Tour tour) {
        TourDTO dto = new TourDTO();
        dto.id = tour.getId();
        dto.deviceFingerprint = tour.getDeviceFingerprint();
        dto.name = tour.getName();
        dto.description = tour.getDescription();
        dto.museum = MuseumDTO.fromEntity(tour.getMuseum());
        dto.theme = tour.getTheme();;
        dto.difficulty = tour.getDifficulty();
        dto.stops = tour.getOrderedStops().stream()
                .map(TourStopDTO::fromEntity)
                .collect(Collectors.toList());
        return dto;
    }
}
