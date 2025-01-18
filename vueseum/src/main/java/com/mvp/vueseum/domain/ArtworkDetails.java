package com.mvp.vueseum.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkDetails {
    private String apiSource;        // Which museum API provided this data
    private String externalId;       // Unique ID from the source museum
    private String title;            // Artwork title

    // Artist Information
    private String artistName;
    private String artistNationality;
    private String artistBirthYear;
    private String artistDeathYear;
    private String artistPrefix;
    private String artistRole;

    // Artwork Specifics
    private String medium;
    private String artworkType;
    private String dimensions;

    // Museum-Specific Metadata
    private String department;       // Museum department
    private Boolean isOnView;        // Current exhibition status
    private String galleryNumber;

    // Additional Descriptive Information
    private String description;      // Artwork description

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private final List<String> tags = new ArrayList<>();
    private String creditLine;

    // Geographical and Cultural Context
    private String culture;
    private String period;
    private String country;
    private String region;
    private String subRegion;
    private String geographyType;

    // Image and Multimedia References
    private String primaryImageUrl;  // Main artwork image

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private final List<String> additionalImageUrls = new ArrayList<>();

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

    public String getFullAttribution() {
        if (!StringUtils.hasText(artistName)) {
            return "Unknown Artist";
        }

        StringBuilder attribution = new StringBuilder();
        if (StringUtils.hasText(artistPrefix)) {
            attribution.append(artistPrefix).append(" ");
        }
        attribution.append(artistName);
        if (StringUtils.hasText(artistRole)) {
            attribution.append(" (").append(artistRole).append(")");
        }
        return attribution.toString();
    }

    public boolean isConfidentAttribution() {
        if (!StringUtils.hasText(artistName)) {
            return false;
        }

        Set<String> uncertainPrefixes = Set.of(
                "Attributed to",
                "Workshop of",
                "Circle of",
                "School of",
                "Style of",
                "After"
        );

        return artistPrefix == null || !uncertainPrefixes.contains(artistPrefix);
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
                ", artistPrefix='" + artistPrefix + '\'' +
                ", artistRole='" + artistRole + '\'' +
                ", medium='" + medium + '\'' +
                ", artworkType='" + artworkType + '\'' +
                ", dimensions='" + dimensions + '\'' +
                ", department='" + department + '\'' +
                ", isOnView=" + isOnView +
                ", galleryNumber='" + galleryNumber + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", creditLine='" + creditLine + '\'' +
                ", culture='" + culture + '\'' +
                ", period='" + period + '\'' +
                ", country='" + country + '\'' +
                ", region='" + region + '\'' +
                ", subRegion='" + subRegion + '\'' +
                ", geographyType='" + geographyType + '\'' +
                ", primaryImageUrl='" + primaryImageUrl + '\'' +
                ", additionalImageUrls=" + additionalImageUrls +
                ", creationYear='" + creationYear + '\'' +
                ", acquisitionDate='" + acquisitionDate + '\'' +
                ", copyrightStatus='" + copyrightStatus + '\'' +
                '}';
    }
}
