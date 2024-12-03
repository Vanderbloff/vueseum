package com.mvp.artplatform.model;


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
@Table(name = "artists")
public class Artist {
    @Id
    @SequenceGenerator(name = "artist_sequence", sequenceName = "artist_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "artist_sequence")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    private String nationality;

    @Column(columnDefinition = "TEXT")
    private String biography;

    private LocalDate birthDate;

    private LocalDate deathDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata;

    public Artist(String artistName,
                  String nationality,
                  String biography,
                  LocalDate birthDate,
                  LocalDate deathDate) {
        this.artistName = artistName;
        this.nationality = nationality;
        this.biography = biography;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", artistName='" + artistName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", biography='" + biography + '\'' +
                ", birthDate=" + birthDate +
                ", deathDate=" + deathDate +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }
}
