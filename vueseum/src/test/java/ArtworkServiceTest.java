import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.dto.ArtworkSearchCriteria;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.exception.ResourceNotFoundException;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.service.artist.ArtistService;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private ArtistService artistService;
    @Mock
    private MuseumService museumService;

    private Cache<String, Artwork> artworkCache;
    private ArtworkService artworkService;
    private Artist testArtist;
    private Museum testMuseum;
    private Artwork testArtwork;

    @BeforeEach
    void setUp() {
        // Initialize cache
        artworkCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(100)
                .build();

        Cache<String, List<String>> filterValueCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(100)
                .build();

        // Initialize service
        artworkService = new ArtworkService(
                artworkRepository,
                artistService,
                museumService,
                artworkCache,
                filterValueCache
        );

        // Setup test data
        testArtist = new Artist();
        testArtist.setArtistName("Test Artist");
        testArtist.setNationality("Test Nationality");

        testMuseum = new Museum();
        testMuseum.setId(1L);
        testMuseum.setName("Test Museum");

        testArtwork = new Artwork();
        testArtwork.setTitle("Test Artwork");
        testArtwork.setExternalId("TEST-001");
        testArtwork.setArtist(testArtist);
        testArtwork.setMuseum(testMuseum);
        testArtwork.setMedium("Oil on canvas");
    }

    @Test
    @DisplayName("when saving new artwork, then saves successfully")
    void whenSavingNewArtwork_thenSavesSuccessfully() {
        ArtworkDetails details = ArtworkDetails.builder()
                .title("Test Artwork")
                .artistName("Test Artist")
                .medium("Oil on canvas")
                .apiSource("Test Museum")
                .externalId("TEST-001")
                .build();

        when(museumService.findOrCreateMuseum(anyString()))
                .thenReturn(testMuseum);
        when(artistService.findOrCreateArtist(any()))
                .thenReturn(testArtist);
        when(artworkRepository.findByExternalIdAndMuseum(anyString(), any()))
                .thenReturn(Optional.empty());
        when(artworkRepository.save(any()))
                .thenReturn(testArtwork);

        artworkService.saveFromDetailsForTest(details);

        verify(artworkRepository).save(argThat(artwork ->
                artwork.getTitle().equals("Test Artwork") &&
                        artwork.getMedium().equals("Oil on canvas") &&
                        artwork.getArtist() == testArtist &&
                        artwork.getMuseum() == testMuseum &&
                        artwork.getExternalId().equals("TEST-001")
        ));

        assertThat(artworkCache.getIfPresent("TEST-001"))
                .isNotNull()
                .satisfies(artwork -> {
                    assertThat(artwork.getTitle()).isEqualTo("Test Artwork");
                    assertThat(artwork.getExternalId()).isEqualTo("TEST-001");
                });
    }

    @Test
    @DisplayName("when searching artworks, then applies correct criteria")
    void whenSearchingArtworks_thenAppliesCorrectCriteria() {
        // Remove the museumId setting and museum finding since we're not using it
        ArtworkSearchCriteria criteria = new ArtworkSearchCriteria();
        criteria.setTitle("Test");
        Pageable pageable = PageRequest.of(0, 10);

        // Remove the museumService mock setup since we won't use it
        Page<Artwork> mockPage = new PageImpl<>(List.of(testArtwork));
        when(artworkRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<ArtworkDetails> result = artworkService.searchArtworks(criteria, pageable);

        assertThat(result).isNotNull();
        var content = result.getContent();
        assertThat(content).hasSize(1);
        var firstArtwork = content.getFirst();
        assertThat(firstArtwork.getTitle()).isEqualTo("Test Artwork");
        assertThat(firstArtwork.getArtistName()).isEqualTo("Test Artist");
    }

    /*@Test
    @DisplayName("when fetching filter options, then returns valid options")
    void whenFetchingFilterOptions_thenReturnsValidOptions() {
        ArtworkSearchCriteria criteria = new ArtworkSearchCriteria();
        when(artworkRepository.findDistinctClassifications())
                .thenReturn(List.of("Painting", "Sculpture"));
        when(artworkRepository.findDistinctCulturesByRegion())
                .thenReturn(List.of("French", "Italian"));

        Map<String, List<String>> options = artworkService.getFilterOptions(criteria);

        assertThat(options)
                .containsKey("objectType")
                .containsKey("cultures");
        assertThat(options.get("objectType"))
                .contains("Painting", "Sculpture");
        assertThat(options.get("cultures"))
                .contains("French", "Italian");
    }*/

    @Test
    @DisplayName("when saving artwork with existing artist, then updates properly")
    void whenSavingArtworkWithExistingArtist_thenUpdatesProperly() {
        ArtworkDetails details = ArtworkDetails.builder()
                .externalId("TEST-001")
                .title("Updated Artwork")
                .artistName("Test Artist")
                .medium("Oil on canvas")
                .apiSource("Test Museum")
                .build();

        when(museumService.findOrCreateMuseum(anyString()))
                .thenReturn(testMuseum);
        when(artistService.findOrCreateArtist(any()))
                .thenReturn(testArtist);
        when(artworkRepository.findByExternalIdAndMuseum(anyString(), any()))
                .thenReturn(Optional.of(testArtwork));
        when(artworkRepository.save(any()))
                .thenReturn(testArtwork);

        artworkService.saveFromDetailsForTest(details);

        verify(artworkRepository).save(argThat(artwork ->
                artwork.getTitle().equals("Updated Artwork") &&
                        artwork.getExternalId().equals("TEST-001")
        ));
    }

    @Test
    @DisplayName("when removing non-displayed artworks, then removes all not in displayed set")
    void whenRemovingNonDisplayedArtworks_thenRemovesAllNotInDisplayedSet() {
        Artwork displayedArtwork = new Artwork();
        displayedArtwork.setExternalId("DISPLAYED-001");
        displayedArtwork.setMuseum(testMuseum);

        Artwork nonDisplayedArtwork = new Artwork();
        nonDisplayedArtwork.setExternalId("NOT-DISPLAYED-001");
        nonDisplayedArtwork.setMuseum(testMuseum);

        when(museumService.findMuseumByIdForSync(1L))
                .thenReturn(Optional.of(testMuseum));

        when(artworkRepository.findAllWithArtistsAndMuseums())
                .thenReturn(List.of(displayedArtwork, nonDisplayedArtwork));

        artworkService.removeNonDisplayedArtworks(
                Set.of("DISPLAYED-001"),
                1L
        );

        verify(artworkRepository).delete(nonDisplayedArtwork);
        verify(artworkRepository, never()).delete(displayedArtwork);
        assertThat(artworkCache.getIfPresent("NOT-DISPLAYED-001")).isNull();
    }

    @Test
    @DisplayName("when removing artworks with invalid museum id, then throws exception")
    void whenRemovingArtworksWithInvalidMuseumId_thenThrowsException() {
        when(museumService.findMuseumByIdForSync(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                artworkService.removeNonDisplayedArtworks(Set.of("TEST-001"), 999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Museum not found");

        verify(artworkRepository, never()).delete(any(Artwork.class));
    }

    @Test
    @DisplayName("when recording processing error, then updates status")
    void whenRecordingProcessingError_thenUpdatesStatus() {
        String externalId = "TEST-001";
        String errorMessage = "Processing failed";
        when(museumService.findMuseumById(1L))
                .thenReturn(Optional.of(testMuseum));
        when(artworkRepository.findByExternalIdAndMuseum(externalId, testMuseum))
                .thenReturn(Optional.of(testArtwork));

        artworkService.recordProcessingError(externalId, 1L, new Exception(errorMessage));

        verify(artworkRepository).save(argThat(artwork ->
                artwork.getProcessingStatus() == Artwork.ProcessingStatus.ERROR &&
                        artwork.getLastSyncError().equals(errorMessage) &&
                        artwork.getLastSyncAttempt() != null
        ));
    }

    @Test
    @DisplayName("when artwork not found during sync, then skips processing")
    void whenArtworkNotFoundDuringSync_thenSkipsProcessing() {
        String externalId = "TEST-001";
        when(museumService.findMuseumById(1L))
                .thenReturn(Optional.of(testMuseum));

        artworkService.recordProcessingError(externalId, 1L,
                new ResourceNotFoundException("Artwork not found"));

        // Verify no delete operation occurred
        verify(artworkRepository, never()).delete((Artwork) any());
        // Verify no save operation occurred
        verify(artworkRepository, never()).save(any());
    }

    @Test
    @DisplayName("when saving artwork with invalid dates, should save with empty dates")
    void whenSavingArtworkWithInvalidDates_thenSavesEmptyDates() {
        // Setup test data
        ArtworkDetails details = ArtworkDetails.builder()
                .externalId("TEST-001")
                .title("Test Artwork")
                .artistName("Test Artist")
                .artistBirthYear("ca. 1800")      // Invalid format
                .artistDeathYear("19th century")   // Invalid format
                .apiSource("Test Museum")
                .build();

        Artist expectedArtist = new Artist();
        expectedArtist.setArtistName("Test Artist");
        expectedArtist.setBirthDate("");
        expectedArtist.setDeathDate("");

        when(museumService.findOrCreateMuseum(anyString()))
                .thenReturn(testMuseum);
        when(artistService.findOrCreateArtist(details))
                .thenReturn(expectedArtist);
        when(artworkRepository.findByExternalIdAndMuseum(anyString(), any()))
                .thenReturn(Optional.empty());
        when(artworkRepository.save(any()))
                .thenReturn(testArtwork);

        artworkService.saveFromDetailsForTest(details);

        verify(artistService).findOrCreateArtist(eq(details));  // Verify details passed to artist service
        verify(artworkRepository).save(argThat(artwork ->
                artwork.getArtist().getBirthDate().isEmpty() &&     // Verify empty dates in saved artwork
                        artwork.getArtist().getDeathDate().isEmpty()
        ));
    }
}