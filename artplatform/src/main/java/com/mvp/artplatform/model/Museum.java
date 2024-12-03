package com.mvp.artplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "museums")
public class Museum {

    @Id
    @SequenceGenerator(name = "museum_sequence", sequenceName = "museum_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "museum_sequence")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(name = "website_url")
    private String websiteUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata;

    public Museum(String name,
                  String location,
                  String websiteUrl,
                  Map<String, Object> additionalMetadata) {
        this.name = name;
        this.location = location;
        this.websiteUrl = websiteUrl;
        this.additionalMetadata = additionalMetadata;
    }

    @Override
    public String toString() {
        return "Museum{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", websiteUrl='" + websiteUrl + '\'' +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }
}
