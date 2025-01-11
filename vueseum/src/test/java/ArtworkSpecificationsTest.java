import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.specification.ArtworkSpecifications;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtworkSpecificationsTest {

    @Mock
    private Root<Artwork> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path<String> titlePath;
    @Mock
    private Path<String> culturePath;
    @Mock
    private Path<String> mediumPath;
    @Mock
    private Path<String> classificationPath;
    @Mock
    private Path<Boolean> displayPath;
    @Mock
    private Join<Artwork, Artist> artistJoin;
    @Mock
    private Join<Artwork, Museum> museumJoin;
    @Mock
    private Expression<String> lowerTitlePath;
    @Mock
    private Predicate likePredicate;
    @Mock
    private Predicate andPredicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.<String>get("title")).thenReturn(titlePath);
        lenient().when(root.<String>get("culture")).thenReturn(culturePath);
        lenient().when(root.<String>get("medium")).thenReturn(mediumPath);
        lenient().when(root.<String>get("classification")).thenReturn(classificationPath);
        lenient().when(root.<Boolean>get("isOnDisplay")).thenReturn(displayPath);
        lenient().when(root.<Artwork, Artist>join("artist")).thenReturn(artistJoin);
        lenient().when(root.<Artwork, Museum>join("museum")).thenReturn(museumJoin);
    }

    @Test
    void whenSearchingByTitle_thenGeneratesCorrectPredicate() {
        // Given
        ArtworkSearchCriteria criteria = ArtworkSearchCriteria.builder()
                .title("Test Artwork")
                .build();

        // Mock the chain of operations
        when(cb.lower(titlePath)).thenReturn(lowerTitlePath);
        when(cb.like(lowerTitlePath, "%test artwork%")).thenReturn(likePredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        // When
        Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
        Predicate result = spec.toPredicate(root, query, cb);

        // Then
        assertThat(result).isNotNull();
        verify(cb).lower(titlePath);
        verify(cb).like(lowerTitlePath, "%test artwork%");
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void whenSearchingByCulture_thenGeneratesDirectMatchPredicate() {
        // Given
        ArtworkSearchCriteria criteria = ArtworkSearchCriteria.builder()
                .culture("Japanese")
                .build();

        // Mock the chain of operations
        when(cb.lower(culturePath)).thenReturn(culturePath);
        when(cb.equal(culturePath, "japanese")).thenReturn(likePredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        // When
        Specification<Artwork> spec = ArtworkSpecifications.withSearchCriteria(criteria);
        Predicate result = spec.toPredicate(root, query, cb);

        // Then
        assertThat(result).isNotNull();
        verify(cb).lower(culturePath);
        verify(cb).equal(culturePath, "japanese");
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void whenGeneratingTourWithTheme_thenAppliesThemeSpecificFilters() {
        // Given
        Long museumId = 1L;
        Tour.TourTheme theme = Tour.TourTheme.CHRONOLOGICAL;

        // Mock paths
        Path creationDatePath = mock(Path.class);
        when(root.<String>get("creationDate")).thenReturn(creationDatePath);

        // Mock museum ID path
        Path museumIdPath = mock(Path.class);
        when(museumJoin.get("id")).thenReturn(museumIdPath);

        // Mock predicates
        Predicate notNullPredicate = mock(Predicate.class);
        Predicate notEmptyPredicate = mock(Predicate.class);
        Predicate museumPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        // Setup predicate chain
        when(cb.isNotNull(creationDatePath)).thenReturn(notNullPredicate);
        when(cb.notEqual(creationDatePath, "")).thenReturn(notEmptyPredicate);
        when(cb.equal(museumIdPath, museumId)).thenReturn(museumPredicate);

        // Important: Mock the specific combination of predicates
        when(cb.and(notNullPredicate, notEmptyPredicate)).thenReturn(finalPredicate);
        when(cb.and(museumPredicate, finalPredicate)).thenReturn(finalPredicate);

        // When
        Specification<Artwork> spec = ArtworkSpecifications.getThemeSpecificPreFilter(theme, museumId);
        Predicate result = spec.toPredicate(root, query, cb);

        // Then
        assertThat(result).isNotNull();
        verify(cb).isNotNull(creationDatePath);
        verify(cb).notEqual(creationDatePath, "");
        verify(museumJoin).get("id");
        verify(cb).equal(museumIdPath, museumId);
    }
}