package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "artworks",
        uniqueConstraints = {@UniqueConstraint(name = "external_id_unique", columnNames = {"external_id", "museum_id"})
    })
public class Artwork extends BaseEntity {

    private static final Set<String> UNCERTAINTY_PREFIXES = Set.of(
            "Attributed to", "Workshop of", "Circle of",
            "School of", "Style of", "After"
    );

    @Column(nullable = false)
    private String title = "";

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    private String medium;

    private String culture;

    // Geographic Information - May be partially or fully empty depending on museum
    @Column(name = "country")
    private String country;  // Standard geographic info, commonly available

    @Column(name = "region")
    private String region;   // More detailed geographic info, may be empty

    @Column(name = "sub_region")
    private String subRegion;  // Met-specific geographic detail

    @Column(name = "geography_type")
    private String geographyType;  // Met-specific geographic categorization

    // Rights Management - Handling varies by museum
    private String copyrightStatus;  // Rights information, format varies by museum

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "gallery_number")
    private String galleryNumber;

    private String department;

    @Column(name = "creation_date")
    private String creationDate;

    @Column(name = "external_id", nullable = false)
    private String externalId = "";

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "museum_id", nullable = false)
    private Museum museum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata = new HashMap<>();

    @Column(name = "classification")
    private String classification;

    // Track processing status
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    // Track last sync attempt
    private LocalDateTime lastSyncAttempt;

    // Track sync error details if any
    @Column(columnDefinition = "TEXT")
    private String lastSyncError;

    @Column(name = "artist_prefix")
    private String artistPrefix;

    @Column(name = "artist_role")
    private String artistRole;


    public enum ProcessingStatus {
        PENDING,
        COMPLETED,
        ERROR
    }

    public Artwork(String title,
                   Artist artist,
                   Museum museum,
                   String externalId,
                   String classification,
                   String medium,
                   String imageUrl) {
        this.title = title;
        this.artist = artist;
        this.museum = museum;
        this.externalId = externalId;
        this.classification = classification;
        this.medium = medium;
        this.imageUrl = imageUrl;
    }

    public String getArtistName() {
        return hasKnownArtist() ? artist.getArtistName() : "Unknown Artist";
    }

    public String getFullAttribution() {
        if (!hasKnownArtist()) {
            return "Unknown Artist";
        }

        // Build the full attribution string regardless of confidence
        StringBuilder attribution = new StringBuilder();
        if (artistPrefix != null) {
            attribution.append(artistPrefix).append(" ");
        }
        attribution.append(artist.getArtistName());
        if (artistRole != null) {
            attribution.append(" (").append(artistRole).append(")");
        }
        return attribution.toString();
    }

    public boolean hasKnownArtist() {
        return artist != null && StringUtils.hasText(artist.getArtistName());
    }

    public boolean isConfidentAttribution() {
        if (artist == null) {
            return false;  // No artist assigned at all
        }

        return artistPrefix == null || !UNCERTAINTY_PREFIXES.contains(artistPrefix);
    }

    public void setMuseum(Museum museum) {
        if (this.museum == museum)
            return;

        if (this.museum != null) {
            this.museum.removeArtwork(this);
        }

        this.museum = museum;
        if (museum != null) {
            museum.addArtwork(this);
        }
    }

    public void setArtist(Artist artist) {
        if (this.artist == artist)
            return;

        if (this.artist != null) {
            this.artist.removeArtwork(this);
        }

        this.artist = artist;
        if (artist != null) {
           artist.addArtwork(this);
        }
    }

    @Override
    public String toString() {
        return "Artwork{" +
                "title='" + title + '\'' +
                ", artist=" + (artist != null ? "Artist{name='" + artist.getArtistName() + "'}" : "Unknown") +
                ", medium='" + medium + '\'' +
                ", culture='" + culture + '\'' +
                ", country='" + country + '\'' +  // Add geographic fields
                ", region='" + region + '\'' +
                ", subRegion='" + subRegion + '\'' +
                ", geographyType='" + geographyType + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", galleryNumber='" + galleryNumber + '\'' +
                ", department='" + department + '\'' +
                ", classification='" + classification + '\'' +
                ", copyrightStatus='" + copyrightStatus + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", externalId='" + externalId + '\'' +
                ", museum=" + (museum != null ? "Museum{name='" + museum.getName() + "'}" : "Unknown") +
                ", additionalMetadata=" + additionalMetadata +
                ", processingStatus=" + processingStatus +
                ", lastSyncAttempt=" + lastSyncAttempt +
                ", lastSyncError='" + lastSyncError + '\'' +
                '}';
    }
}
