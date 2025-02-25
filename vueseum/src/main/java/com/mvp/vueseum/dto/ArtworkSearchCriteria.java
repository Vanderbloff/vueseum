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

    @Size(max = 100)
    private String period;

    @Size(max = 100)
    private String department;

    /**
     * Combined search field for artwork type and medium.
     * This allows searching across multiple classification fields simultaneously.
     */
    @Size(max = 100)
    private String category;

    private Boolean hasImage;

    /**
     * Combined search field for culture, country, and region.
     * This allows searching across multiple geographic and cultural fields simultaneously.
     */
    @Size(max = 100)
    private String origin;

    private Double accessionNumber;

    private Long museumId;

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
