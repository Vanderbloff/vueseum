package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.baseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class Artist extends baseEntity {

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    private String nationality;

    /*@Column(columnDefinition = "TEXT")
    private String biography;*/

    @Pattern(regexp = "^[0-9]+$", message = "Year must be a positive number or zero")
    @Size(min = 4, max = 4, message = "Year must be in YYYY format")
    private String birthDate;

    @Pattern(regexp = "^[0-9]+$", message = "Year must be a positive number or zero")
    @Size(min = 4, max = 4, message = "Year must be in YYYY format")
    private String deathDate;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL)
    private Set<Artwork> works = new HashSet<>();


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata = new HashMap<>();

    public Artist(String artistName,
                  String nationality,
//                  String biography,
                  String birthDate,
                  String deathDate) {
        this.artistName = artistName;
        this.nationality = nationality;
//        this.biography = biography;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
    }

    public void addArtwork(Artwork artwork) {
        works.add(artwork);
        if (artwork.getArtist() != this) {
            artwork.setArtist(this);
        }
    }

    public void removeArtwork(Artwork artwork) {
        works.remove(artwork);
        if (artwork.getArtist() == this) {
            artwork.setArtist(null);
        }
    }

    public Set<Artwork> getWorks() {
        return new HashSet<>(works);
    }

    // Class-level validation for comparing dates
    @AssertTrue(message = "Death year must occur after birth year and within reasonable lifespan")
    public boolean hasValidLifespan() {
        if (birthDate == null || deathDate == null) {
            return true;
        }
        int birth = Integer.parseInt(birthDate);
        int death = Integer.parseInt(deathDate);
        int lifespan = death - birth;
        return lifespan > 0 && lifespan <= 120;
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
