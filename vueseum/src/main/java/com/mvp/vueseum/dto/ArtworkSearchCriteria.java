package com.mvp.vueseum.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkSearchCriteria {

    @Size(max = 100)
    private String title;

    @Pattern(regexp = "^[\\p{L}\\s.-]+$")
    private String artistName;

    @Size(max = 50)
    private String medium;

    @Size(max = 100)
    private String period;

    @Size(max = 100)
    private String culture;

    @Size(max = 100)
    private String department;

    @Size(max = 50)
    private String artworkType;

    private Boolean isOnDisplay;

    @Size(max = 100)
    private String geographicLocation;

    private Double accessionNumber;

    private List<String> museums;  // For future museum additions

    private List<String> tags;
}
