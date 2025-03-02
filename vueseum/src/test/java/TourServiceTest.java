import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mvp.vueseum.domain.TourGenerationRequest;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.domain.TourUpdateRequest;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.event.TourProgressListener;
import com.mvp.vueseum.exception.GenerationLimitExceededException;
import com.mvp.vueseum.exception.TourLimitExceededException;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.repository.TourRepository;
import com.mvp.vueseum.service.DescriptionGenerationService;
import com.mvp.vueseum.service.artwork.ArtworkService;
import com.mvp.vueseum.service.museum.MuseumService;
import com.mvp.vueseum.service.tour.ScoringService;
import com.mvp.vueseum.service.tour.TourService;
import com.mvp.vueseum.service.visitor.DeviceFingerprintService;
import com.mvp.vueseum.service.visitor.VisitorTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {
    @Mock
    private DescriptionGenerationService descriptionService;
    @Mock
    private ArtworkService artworkService;
    @Mock
    private MuseumService museumService;
    @Mock
    private VisitorTrackingService visitorTrackingService;
    @Mock
    private DeviceFingerprintService deviceFingerprintService;
    @Mock
    private ScoringService scoringService;
    @Mock
    private TourProgressListener progressListener;
    @Mock
    private TourRepository tourRepository;
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private HttpServletRequest httpRequest;

    private TourService tourService;
    private Museum testMuseum;
    private List<Artwork> testArtworks;
    private static final String TEST_DEVICE_FINGERPRINT = "test-visitor";

    @BeforeEach
    void setUp() {
        // Initialize cache
        Cache<String, String> descriptionCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(100)
                .build();

        // Initialize service
        tourService = new TourService(
                descriptionService,
                artworkService,
                museumService,
                visitorTrackingService,
                deviceFingerprintService,
                scoringService,
                progressListener,
                tourRepository,
                artworkRepository,
                descriptionCache
        );

        // Setup test data
        testMuseum = new Museum();
        testMuseum.setId(1L);
        testMuseum.setName("Test Museum");

        testArtworks = IntStream.range(0, 5)
                .mapToObj(i -> {
                    Artwork artwork = new Artwork();
                    artwork.setId((long) i);
                    artwork.setTitle("Artwork " + i);
                    artwork.setMuseum(testMuseum);
                    return artwork;
                })
                .collect(Collectors.toList());

        // Common mock setup
        lenient().when(museumService.isValidMuseum(1L)).thenReturn(true);
        lenient().when(deviceFingerprintService.generateFingerprint(any()))
                .thenReturn(TEST_DEVICE_FINGERPRINT);
    }

    @Test
    @DisplayName("when generating description, uses caching")
    void whenGeneratingDescription_thenUsesCaching() {
        String description = "Test description";
        when(descriptionService.generateTourDescription(any(), any()))
                .thenReturn(description);
        when(artworkService.findArtworkCandidates(any()))
                .thenReturn(testArtworks.subList(0, 3));
        when(scoringService.scoreArtwork(any(), any(), any()))
                .thenReturn(1.0);
        when(visitorTrackingService.recordTourGeneration(anyString()))
                .thenReturn(true);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);

        // Mock the save method to return a Tour with an ID
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour savedTour = invocation.getArgument(0);
            savedTour.setId(1L); // Set ID to simulate database persistence
            return savedTour;
        });

        TourGenerationRequest request = createTestRequest();

        Tour firstTour = tourService.generateTour(request, httpRequest);
        Tour secondTour = tourService.generateTour(request, httpRequest);

        verify(descriptionService, times(1))
                .generateTourDescription(any(), any());
        assertThat(secondTour.getDescription())
                .isEqualTo(firstTour.getDescription());

        // Verify save is called twice (once for each tour)
        verify(tourRepository, times(2)).save(any(Tour.class));
    }

    @Test
    @DisplayName("when generating tour, then successful")
    void whenGeneratingTour_thenSuccessful() {
        when(artworkService.findArtworkCandidates(any()))
                .thenReturn(testArtworks.subList(0, 3));
        when(scoringService.scoreArtwork(any(), any(), any()))
                .thenReturn(1.0);
        when(visitorTrackingService.recordTourGeneration(anyString()))
                .thenReturn(true);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);
        when(descriptionService.generateTourDescription(any(), any()))
                .thenReturn("Test description");

        // Mock the save method to return a Tour with an ID
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour savedTour = invocation.getArgument(0);
            savedTour.setId(1L); // Set ID to simulate database persistence
            return savedTour;
        });

        TourGenerationRequest request = createTestRequest();

        Tour result = tourService.generateTour(request, httpRequest);

        assertThat(result)
                .isNotNull()
                .satisfies(tour -> {
                    assertThat(tour.getId()).isEqualTo(1L); // Verify ID is set
                    assertThat(tour.getStops()).hasSize(3);
                    assertThat(tour.getDeviceFingerprint())
                            .isEqualTo(TEST_DEVICE_FINGERPRINT);
                    assertThat(tour.getTheme())
                            .isEqualTo(Tour.TourTheme.CHRONOLOGICAL);
                });

        verify(progressListener).initializeProgress(anyString(), eq("test-visitor"));
        verify(progressListener, atLeastOnce())
                .updateProgress(anyString(), anyDouble(), anyString());
        verify(tourRepository).save(any(Tour.class)); // Verify save is called
    }

    @Test
    @DisplayName("when required artworks provided, then included in tour")
    void whenRequiredArtworksProvided_thenIncludedInTour() {
        TourGenerationRequest request = createTestRequest();
        request.getPreferences().setRequiredArtworkIds(
                Set.of(testArtworks.getFirst().getId())
        );

        when(artworkService.findArtworkCandidates(any()))
                .thenReturn(testArtworks);
        when(scoringService.scoreArtwork(any(), any(), any()))
                .thenReturn(1.0);
        when(visitorTrackingService.recordTourGeneration(anyString()))
                .thenReturn(true);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);
        when(descriptionService.generateTourDescription(any(), any()))
                .thenReturn("Test description");

        // Mock the save method to return a Tour with an ID
        when(tourRepository.save(any(Tour.class))).thenAnswer(invocation -> {
            Tour savedTour = invocation.getArgument(0);
            savedTour.setId(1L); // Set ID to simulate database persistence
            return savedTour;
        });

        Tour result = tourService.generateTour(request, httpRequest);

        assertThat(result.getStops())
                .extracting(stop -> stop.getArtwork().getId())
                .contains(testArtworks.getFirst().getId());

        verify(tourRepository).save(any(Tour.class)); // Verify save is called
    }

    @Test
    @DisplayName("when daily limit reached, then throws exception")
    void whenDailyLimitReached_thenThrowsException() {
        when(visitorTrackingService.recordTourGeneration(anyString()))
                .thenReturn(false);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);

        assertThatThrownBy(() ->
                tourService.generateTour(createTestRequest(), httpRequest))
                .isInstanceOf(GenerationLimitExceededException.class)
                .hasMessageContaining("Daily tour generation limit");
    }

    @Test
    @DisplayName("when device limit reached, then throws exception")
    void whenDeviceLimitReached_thenThrowsException() {
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(TEST_DEVICE_FINGERPRINT))
                .thenReturn(10L);

        assertThatThrownBy(() ->
                tourService.generateTour(createTestRequest(), httpRequest))
                .isInstanceOf(TourLimitExceededException.class)
                .hasMessageContaining("Maximum tour limit");
    }

    @Test
    @DisplayName("when updating tour, then only mutable fields change")
    void whenUpdatingTour_thenOnlyMutableFieldsChange() {
        Tour existingTour = new Tour();
        existingTour.setName("Original Name");
        existingTour.setDescription("Original Description");
        existingTour.setTheme(Tour.TourTheme.CHRONOLOGICAL);
        existingTour.setMuseum(testMuseum);

        when(tourRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(existingTour));
        when(tourRepository.save(any(Tour.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TourUpdateRequest updateRequest =
                new TourUpdateRequest("New Name", "New Description");

                Optional<Tour> result = tourService.updateTourDetails(1L, updateRequest);

        assertThat(result)
                .isPresent()
                .hasValueSatisfying(tour -> {
                    assertThat(tour.getName()).isEqualTo("New Name");
                    assertThat(tour.getDescription()).isEqualTo("New Description");
                    assertThat(tour.getTheme()).isEqualTo(Tour.TourTheme.CHRONOLOGICAL);
                    assertThat(tour.getMuseum()).isEqualTo(testMuseum);
                });
    }

    private TourGenerationRequest createTestRequest() {
        return TourGenerationRequest.builder()
                .visitorId("test-visitor")
                .preferences(TourPreferences.builder()
                        .museumId(1L)
                        .theme(Tour.TourTheme.CHRONOLOGICAL)
                        .maxStops(5)
                        .minStops(3)
                        .build())
                .build();
    }
}