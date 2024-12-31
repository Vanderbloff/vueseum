package com.mvp.vueseum.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkDetails {
    private String apiSource;        // Which museum API provided this data
    private String externalId;       // Unique ID from the source museum
    private String title;            // Artwork title

    // Artist Information
    private String artistName;       // Primary artist name
    private String artistNationality;// Artist's country of origin
    private String artistBirthYear;
    private String artistDeathYear;
    private String artistPrefix;
    private String artistRole;

    // Artwork Specifics
    private String medium;           // Painting, sculpture, etc.
    private String artworkType;      // Classification of artwork
    private String dimensions;       // Physical size description

    // Museum-Specific Metadata
    private String department;       // Museum department
    private Boolean isOnView;        // Current exhibition status
    private String currentLocation;  // Museum/gallery location
    private String galleryNumber;

    // Additional Descriptive Information
    private String description;      // Artwork description

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private List<String> tags = new ArrayList<>();       // Searchable keywords
    private String creditLine;       // Museum attribution

    // Geographical and Cultural Context
    private String culture;          // Cultural origin
    private String period;           // Historical period

    // Image and Multimedia References
    private String primaryImageUrl;  // Main artwork image

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private List<String> additionalImageUrls = new ArrayList<>();

    // Acquisition and Provenance
    private String creationYear;
    private String acquisitionDate;
    private String copyrightStatus;

    @JsonProperty("tags")
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    @JsonProperty("additionalImageUrls")
    public List<String> getAdditionalImageUrls() {
        return new ArrayList<>(additionalImageUrls);
    }

    public String getArtistName() {
        return StringUtils.hasText(artistName) ? artistName : "Unknown Artist";
    }

    @Override
    public String toString() {
        return "ArtworkDetails{" +
                "apiSource='" + apiSource + '\'' +
                ", externalId='" + externalId + '\'' +
                ", title='" + title + '\'' +
                ", artistName='" + artistName + '\'' +
                ", artistNationality='" + artistNationality + '\'' +
                ", artistBirthYear='" + artistBirthYear + '\'' +
                ", artistDeathYear='" + artistDeathYear + '\'' +
                ", medium='" + medium + '\'' +
                ", artworkType='" + artworkType + '\'' +
                ", dimensions='" + dimensions + '\'' +
                ", department='" + department + '\'' +
                ", isOnView=" + isOnView +
                ", currentLocation='" + currentLocation + '\'' +
                ", galleryNumber='" + galleryNumber + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", creditLine='" + creditLine + '\'' +
                ", culture='" + culture + '\'' +
                ", period='" + period + '\'' +
                ", primaryImageUrl='" + primaryImageUrl + '\'' +
                ", additionalImageUrls=" + additionalImageUrls +
                ", creationYear='" + creationYear + '\'' +
                ", acquisitionDate='" + acquisitionDate + '\'' +
                ", copyrightStatus='" + copyrightStatus + '\'' +
                '}';
    }
}
