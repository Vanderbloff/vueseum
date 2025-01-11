import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {
    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ArtistService artistService;

    @Mock
    private MuseumService museumService;

    private Cache<String, Artwork> artworkCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .maximumSize(1000)
            .build();

    @InjectMocks
    private ArtworkService artworkService;

    private Artist testArtist;
    private Museum testMuseum;
    private Artwork testArtwork;

    @BeforeEach
    void setUp() {
        artworkCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofDays(1))
                .maximumSize(1000)
                .recordStats()
                .scheduler(Scheduler.systemScheduler())
                .build();

        ReflectionTestUtils.setField(artworkService, "artworkCache", artworkCache);

        artworkCache.invalidateAll();

        testArtist = new Artist();
        testArtist.setArtistName("Test Artist");
        testArtist.setNationality("Test Nationality");

        testMuseum = new Museum();
        testMuseum.setId(1L);
        testMuseum.setName("Test Museum");
        testMuseum.setLocation("Test Location");

        testArtwork = new Artwork();
        testArtwork.setTitle("Test Artwork");
        testArtwork.setExternalId("TEST-001");
        testArtwork.setMedium("Oil on canvas");
        testArtwork.setArtist(testArtist);
        testArtwork.setMuseum(testMuseum);
    }

    @Test
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

        Artwork savedArtwork = new Artwork();
        savedArtwork.setTitle("Test Artwork");
        savedArtwork.setExternalId("TEST-001");
        savedArtwork.setArtist(testArtist);
        savedArtwork.setMuseum(testMuseum);

        when(artworkRepository.save(any()))
                .thenReturn(savedArtwork);

        artworkService.saveFromDetailsForTest(details);

        verify(artworkRepository).save(argThat(artwork ->
                artwork.getTitle().equals("Test Artwork") &&
                        artwork.getMedium().equals("Oil on canvas") &&
                        artwork.getArtist() == testArtist &&
                        artwork.getMuseum() == testMuseum &&
                        artwork.getExternalId().equals("TEST-001")  // Add this
        ));

        assertThat(artworkCache.getIfPresent("TEST-001"))
                .isNotNull()
                .satisfies(artwork -> {
                    assertThat(artwork.getTitle()).isEqualTo("Test Artwork");
                    assertThat(artwork.getExternalId()).isEqualTo("TEST-001");
                });
    }

    @Test
    void whenSearchingArtworks_thenAppliesCorrectCriteria() {
        ArtworkSearchCriteria criteria = new ArtworkSearchCriteria();
        criteria.setTitle("Test");
        Pageable pageable = PageRequest.of(0, 10);

        Page<Artwork> mockPage = new PageImpl<>(List.of(testArtwork));
        when(artworkRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<ArtworkDetails> result = artworkService.searchArtworks(criteria, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(1)
                .first()
                .satisfies(details -> {
                    assertThat(details.getTitle()).isEqualTo("Test Artwork");
                    assertThat(details.getArtistName()).isEqualTo("Test Artist");
                });
    }

    @Test
    void whenFetchingFilterOptions_thenReturnsValidOptions() {
        ArtworkSearchCriteria criteria = new ArtworkSearchCriteria();
        when(artworkRepository.findDistinctClassifications())
                .thenReturn(List.of("Painting", "Sculpture"));
        when(artworkRepository.findDistinctCultures())
                .thenReturn(List.of("French", "Italian"));

        Map<String, List<String>> options = artworkService.getFilterOptions(criteria);

        assertThat(options)
                .containsKey("objectType")
                .containsKey("cultures");
        assertThat(options.get("objectType"))
                .contains("Painting", "Sculpture");
        assertThat(options.get("cultures"))
                .contains("French", "Italian");
    }

    @Test
    void whenSavingArtworkWithExistingArtist_thenUpdatesProperly() {
        ArtworkDetails details = ArtworkDetails.builder()
                .externalId("TEST-001")
                .title("Updated Artwork")
                .artistName("Test Artist")
                .medium("Oil on canvas")
                .apiSource("Test Museum")
                .build();

        when(museumService.findOrCreateMuseum(anyString())).thenReturn(testMuseum);
        when(artistService.findOrCreateArtist(any())).thenReturn(testArtist);
        when(artworkRepository.findByExternalIdAndMuseum(anyString(), any())).thenReturn(Optional.of(testArtwork));
        when(artworkRepository.save(any())).thenReturn(testArtwork);

        artworkService.saveFromDetailsForTest(details);

        verify(artworkRepository).save(argThat(artwork ->
                artwork.getTitle().equals("Updated Artwork") &&
                        artwork.getExternalId().equals("TEST-001")
        ));
    }

    @Test
    void whenFetchingArtworkById_thenReturnsCorrectArtwork() {
        String externalId = "TEST-001";
        Long museumId = 1L;

        when(museumService.findMuseumById(museumId)).thenReturn(Optional.of(testMuseum));
        when(artworkRepository.findByExternalIdAndMuseum(externalId, testMuseum))
                .thenReturn(Optional.of(testArtwork));

        ArtworkDetails result = artworkService.fetchArtworkById(externalId, museumId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Artwork");
        assertThat(result.getArtistName()).isEqualTo("Test Artist");
    }

    @Test
    void whenFetchingNonExistentArtwork_thenThrowsException() {
        String externalId = "NONEXISTENT";
        Long museumId = 1L;

        when(museumService.findMuseumById(museumId)).thenReturn(Optional.of(testMuseum));
        when(artworkRepository.findByExternalIdAndMuseum(externalId, testMuseum))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> artworkService.fetchArtworkById(externalId, museumId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Artwork not found");
    }

    @Test
    void whenCheckingDisplayStatus_thenUpdatesCorrectly() {
        Long artworkId = 1L;
        when(artworkRepository.findById(artworkId)).thenReturn(Optional.of(testArtwork));

        artworkService.updateDisplayStatus(artworkId, true);

        verify(artworkRepository).save(argThat(artwork ->
                artwork.getIsOnDisplay() &&
                        artwork.getDisplayStatusCheck() != null
        ));
    }

    @Test
    void whenSavingArtwork_thenCacheIsUpdated() {
        ArtworkDetails details = ArtworkDetails.builder()
                .externalId("TEST-001")
                .title("Test Artwork")
                .artistName("Test Artist")
                .medium("Oil on canvas")
                .apiSource("Test Museum")
                .build();

        when(museumService.findOrCreateMuseum(anyString())).thenReturn(testMuseum);
        when(artistService.findOrCreateArtist(any())).thenReturn(testArtist);
        when(artworkRepository.findByExternalIdAndMuseum(anyString(), any()))
                .thenReturn(Optional.empty());
        when(artworkRepository.save(any())).thenReturn(testArtwork);

        artworkService.saveFromDetailsForTest(details);

        Artwork cachedArtwork = artworkCache.getIfPresent("TEST-001");
        assertThat(cachedArtwork)
                .isNotNull()
                .satisfies(artwork -> {
                    assertThat(artwork.getTitle()).isEqualTo("Test Artwork");
                    assertThat(artwork.getExternalId()).isEqualTo("TEST-001");
                });
    }

    @Test
    void whenUpdatingExistingArtwork_thenCacheIsUpdated() {
        String externalId = "TEST-001";

        // Pre-populate cache
        artworkCache.put(externalId, testArtwork);

        ArtworkDetails updatedDetails = ArtworkDetails.builder()
                .externalId(externalId)
                .title("Updated Artwork")
                .artistName("Test Artist")
                .medium("Oil on canvas")
                .apiSource("Test Museum")
                .build();

        when(museumService.findOrCreateMuseum(anyString())).thenReturn(testMuseum);
        when(artistService.findOrCreateArtist(any())).thenReturn(testArtist);
        when(artworkRepository.findByExternalIdAndMuseum(anyString(), any()))
                .thenReturn(Optional.of(testArtwork));
        when(artworkRepository.save(any())).thenReturn(testArtwork);

        artworkService.saveFromDetailsForTest(updatedDetails);

        Artwork cachedArtwork = artworkCache.getIfPresent(externalId);
        assertThat(cachedArtwork)
                .isNotNull()
                .satisfies(artwork ->
                        assertThat(artwork.getTitle()).isEqualTo("Updated Artwork")
                );
    }

    @Test
    void whenCachingArtwork_thenRespectsMaxSize() {
        int maxCacheEntries = 1000;

        // Create and cache more than the maximum entries
        for (int i = 0; i < maxCacheEntries + 10; i++) {
            Artwork artwork = new Artwork();
            artwork.setExternalId("TEST-" + i);
            artwork.setTitle("Test Artwork " + i);
            artworkCache.put(artwork.getExternalId(), artwork);
        }

        // Force cache maintenance
        artworkCache.cleanUp();

        assertThat(artworkCache.estimatedSize())
                .isLessThanOrEqualTo((long)(maxCacheEntries * 1.01)); // Allow 1% buffer
    }
}