package com.mvp.artplatform.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkSearchCriteria {
    private String title;
    private String artistName;
    private String medium;
    private String period;
    private String culture;
    private String department;
    private String artworkType;
    private LocalDate createdAfter;
    private LocalDate createdBefore;
    private Boolean isOnDisplay;
    private String geographicLocation;
    private Double accessionNumber;
    private List<String> museums;  // For future museum additions
}
