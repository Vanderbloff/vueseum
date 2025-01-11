package com.mvp.vueseum.entity;

import com.mvp.vueseum.entity.base.baseEntity;
import jakarta.persistence.*;
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
@Table(name = "museums")
public class Museum extends baseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(name = "website_url")
    private String websiteUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, MuseumHours> museumHours = new HashMap<>();

    @Column(name = "artworks")
    @OneToMany(mappedBy = "museum", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Artwork> collection = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalMetadata = new HashMap<>();

    public Museum(String name,
                  String location,
                  String websiteUrl,
                  Map<String, Object> additionalMetadata) {
        this.name = name;
        this.location = location;
        this.websiteUrl = websiteUrl;
        this.additionalMetadata = additionalMetadata;
    }

    @Getter @Setter
    public static class MuseumHours {
        private String open;
        private String close;
        private Boolean closed;
        private String notes; // For special instructions
    }

    public void addArtwork(Artwork artwork) {
        if (artwork != null) {
            collection.add(artwork);
            if (artwork.getMuseum() != this) {
                artwork.setMuseum(this);
            }
        }
    }

    public void removeArtwork(Artwork artwork) {
        if (artwork != null && collection.contains(artwork)) {
            collection.remove(artwork);
            if (artwork.getMuseum() == this) {
                artwork.setMuseum(null);
            }
        }
    }

    public Set<Artwork> getCollection() {
        return new HashSet<>(collection);
    }

    @Override
    public String toString() {
        return "Museum{" +
                "name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", websiteUrl='" + websiteUrl + '\'' +
                ", museumHours=" + museumHours +
                ", collectionCount=" + collection.size() +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }
}
