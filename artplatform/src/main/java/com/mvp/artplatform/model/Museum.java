package com.mvp.artplatform.model;

import com.mvp.artplatform.model.base.baseEntity;
import jakarta.persistence.*;
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
public class Museum extends baseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(name = "website_url")
    private String websiteUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, museumHours> museumHours;

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
                "name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", websiteUrl='" + websiteUrl + '\'' +
                ", museumHours=" + museumHours +
                ", additionalMetadata=" + additionalMetadata +
                '}';
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    @Getter @Setter
    public class museumHours {
        private String open;
        private String close;
        private String notes; // For special instructions
    }
}
