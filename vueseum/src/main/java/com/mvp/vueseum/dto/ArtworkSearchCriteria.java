package com.mvp.vueseum.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkSearchCriteria {

    @Size(max = 100)
    private String title;

    @Pattern(regexp = "^[\\p{L}\\s.-]+$")
    private String artistName;

    @Size(max = 50)
    private String medium;

    @Size(max = 100)
    private String period;

    @Size(max = 100)
    private String culture;

    @Size(max = 100)
    private String department;

    @Size(max = 50)
    private String artworkType;

    private Boolean hasImage;

    @Size(max = 100)
    private String geographicLocation;

    private Double accessionNumber;

    private List<String> museums;

    private List<String> tags;

    @Pattern(regexp = "^(relevance|title|artist|date)$")
    @Getter(AccessLevel.NONE)
    private String sortField;

    @Pattern(regexp = "^(asc|desc)$")
    @Getter(AccessLevel.NONE)
    private String sortDirection;

    public Sort.Direction getSortDirection() {
        if (sortDirection == null) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.fromString(sortDirection);
    }

    public String getSortField() {
        return sortField != null ? sortField : "relevance";
    }

    public static String mapSortField(String field) {
        return switch (field) {
            case "title" -> "title";
            case "artist" -> "artist.artistName";
            case "date" -> "creationDate";
            default -> "id";
        };
    }
}
