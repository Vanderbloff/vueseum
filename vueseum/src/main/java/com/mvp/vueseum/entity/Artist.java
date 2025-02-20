package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "artists")
public class Artist extends BaseEntity {

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    private String nationality;

    @Pattern(regexp = "^$|^[0-9]{4}$", message = "Year must be in YYYY format")
    private String birthDate;

    @Pattern(regexp = "^$|^[0-9]{4}$", message = "Year must be in YYYY format")
    private String deathDate;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL)
    private Set<Artwork> works = new HashSet<>();


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata = new HashMap<>();

    public Artist(String artistName,
                  String nationality,
                  String birthDate,
                  String deathDate) {
        this.artistName = artistName;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
    }

    public void addArtwork(Artwork artwork) {
        if (artwork != null) {
            works.add(artwork);
            if (artwork.getArtist() != this) {
                artwork.setArtist(this);
            }
        }
    }

    public void removeArtwork(Artwork artwork) {
        if (artwork != null && works.contains(artwork)) {
            works.remove(artwork);
            if (artwork.getArtist() == this) {
                artwork.setArtist(null);
            }
        }
    }

    public Set<Artwork> getWorks() {
        return new HashSet<>(works);
    }

    // Class-level validation for comparing dates
    @AssertTrue(message = "Death year must occur after birth year and within reasonable lifespan")
    public boolean hasValidLifespan() {

        if (birthDate == null || birthDate.isEmpty() ||
                deathDate == null || deathDate.isEmpty()) {
            return true;  // No dates = valid (can't validate)
        }
        try {
            int birth = Integer.parseInt(birthDate);
            int death = Integer.parseInt(deathDate);

            // Basic validation rules:
            // 1. Death must be after birth
            // 2. Lifespan should be reasonable (e.g., less than 120 years)
            // 3. Dates should be within reasonable historical range (e.g., after year 1000)
            return death > birth &&
                    (death - birth) <= 120 &&
                    birth >= 1000;
        } catch (NumberFormatException e) {
            // If we can't parse the dates, consider it valid
            // This handles cases where dates might be in different formats
            return true;
        }
    }

    @Override
    public String toString() {
        return "Artist{" +
                "artistName='" + artistName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", deathDate='" + deathDate + '\'' +
                ", worksCount=" + works.size() +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }
}
