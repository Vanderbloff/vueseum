package com.mvp.artplatform.model;

import com.mvp.artplatform.model.base.baseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
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
    private String title;

    @ManyToOne
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    private String medium;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "gallery_number")
    private String GalleryNumber;

    private String country;

    @Column(name = "creation_date")
    private String creationDate;

    @Column(name = "on_display", nullable = false)
    private Boolean isOnDisplay;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @ManyToOne
    @JoinColumn(name = "museum_id", nullable = false)
    private Museum museum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata;

    public Artwork(String title,
                   Artist artist,
                   String medium,
                   String imageUrl,
                   String description,
                   String GalleryNumber,
                   String country,
                   String creationDate,
                   Boolean isOnDisplay) {
        this.title = title;
        this.artist = artist;
        this.medium = medium;
        this.imageUrl = imageUrl;
        this.description = description;
        this.GalleryNumber = GalleryNumber;
        this.country = country;
        this.creationDate = creationDate;
        this.isOnDisplay = isOnDisplay;
    }

    @Override
    public String toString() {
        return "Artwork{" +
                "title='" + title + '\'' +
                ", artist=" + artist +
                ", medium='" + medium + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", GalleryNumber='" + GalleryNumber + '\'' +
                ", country='" + country + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", isOnDisplay=" + isOnDisplay +
                ", externalId='" + externalId + '\'' +
                ", museum=" + museum +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }
}
