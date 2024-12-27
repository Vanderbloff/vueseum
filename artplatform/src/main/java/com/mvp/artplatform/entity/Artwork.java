package com.mvp.artplatform.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.mvp.artplatform.entity.base.baseEntity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "artworks",
        uniqueConstraints = {@UniqueConstraint(name = "external_id_unique", columnNames = {"external_id", "museum_id"})
    })
public class Artwork extends baseEntity {

    @Column(nullable = false)
    private String title = "";

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    private String medium;

    private String culture;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "gallery_number")
    private String GalleryNumber;

    private String currentLocation;

    @Column(name = "creation_date")
    private String creationDate;

    @Column(name = "on_display", nullable = false)
    private Boolean isOnDisplay = false;

    @Column(name = "external_id", nullable = false)
    private String externalId = "";

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "museum_id", nullable = false)
    private Museum museum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata = new HashMap<>();

    @Column(name = "type")
    private String classification;

    private LocalDateTime displayStatusCheck;

    // Track processing status
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    // Track last sync attempt
    private LocalDateTime lastSyncAttempt;

    // Track sync error details if any
    @Column(columnDefinition = "TEXT")
    private String lastSyncError;

    @Column(name = "attribution_certainty")
    private String attributionCertainty;  // e.g., "Attributed to", "Workshop of", "Circle of"

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        ERROR
    }

    public Artwork(String title,
                   Artist artist,
                   String medium,
                   String imageUrl,
                   String description,
                   String GalleryNumber,
                   String currentLocation,
                   String creationDate,
                   Boolean isOnDisplay,
                   LocalDateTime displayStatusCheck) {
        this.title = title;
        this.artist = artist;
        this.medium = medium;
        this.imageUrl = imageUrl;
        this.description = description;
        this.GalleryNumber = GalleryNumber;
        this.currentLocation = currentLocation;
        this.creationDate = creationDate;
        this.isOnDisplay = isOnDisplay;
        this.displayStatusCheck = displayStatusCheck;
    }

    public String getArtistName() {
        return hasKnownArtist() ? artist.getArtistName() : "Unknown Artist";
    }

    public boolean hasKnownArtist() {
        return artist != null && StringUtils.hasText(artist.getArtistName());
    }

    public boolean isConfidentAttribution() {
        return artist != null &&
                (attributionCertainty == null ||
                        attributionCertainty.equals("By") ||
                        attributionCertainty.equals("Signed"));
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
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", GalleryNumber='" + GalleryNumber + '\'' +
                ", currentLocation='" + currentLocation + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", isOnDisplay=" + isOnDisplay +
                ", externalId='" + externalId + '\'' +
                ", museum=" + (museum != null ? "Museum{name='" + museum.getName() + "'}" : "Unknown") +
                ", additionalMetadata=" + additionalMetadata +
                ", displayStatusCheck=" + displayStatusCheck +
                ", processingStatus=" + processingStatus +
                ", lastSyncAttempt=" + lastSyncAttempt +
                ", lastSyncError='" + lastSyncError + '\'' +
                '}';
    }
}
