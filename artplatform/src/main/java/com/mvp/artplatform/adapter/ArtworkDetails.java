package com.mvp.artplatform.adapter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkDetails {
    private String apiSource;        // Which museum API provided this data
    private String externalId;       // Unique ID from the source museum
    private String title;            // Artwork title

    // Artist Information
    private String artistName;       // Primary artist name
    private String artistNationality;// Artist's country of origin
    private Integer artistBirthYear;
    private Integer artistDeathYear;

    // Artwork Specifics
    private String medium;           // Painting, sculpture, etc.
    private String artworkType;      // Classification of artwork
    private String dimensions;       // Physical size description

    // Museum-Specific Metadata
    private String department;       // Museum department
    private Boolean isOnView;        // Current exhibition status
    private String currentLocation;  // Museum/gallery location

    // Additional Descriptive Information
    private String description;      // Artwork description
    private List<String> tags;       // Searchable keywords
    private String creditLine;       // Museum attribution

    // Geographical and Cultural Context
    private String culture;          // Cultural origin
    private String period;           // Historical period

    // Image and Multimedia References
    private String primaryImageUrl;  // Main artwork image
    private List<String> additionalImageUrls;

    // Acquisition and Provenance
    private Integer creationYear;
    private String acquisitionDate;
    private String copyrightStatus;
}
