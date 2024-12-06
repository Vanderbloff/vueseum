package com.mvp.artplatform.model;


import com.mvp.artplatform.model.base.baseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Year;
import java.util.Map;



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

    private String birthDate;

    private String deathDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata;

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

    @Override
    public String toString() {
        return "Artist{" +
                "artistName='" + artistName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", birthDate=" + birthDate +
                ", deathDate=" + deathDate +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }
}
