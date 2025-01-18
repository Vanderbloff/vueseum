import com.mvp.vueseum.domain.TourGenerationRequest;
import com.mvp.vueseum.domain.TourPreferences;
import com.mvp.vueseum.domain.TourUpdateRequest;
import com.mvp.vueseum.entity.Artwork;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.entity.Tour;
import com.mvp.vueseum.entity.base.baseEntity;
import com.mvp.vueseum.event.TourProgressListener;
import com.mvp.vueseum.exception.GenerationLimitExceededException;
import com.mvp.vueseum.exception.InvalidRequestException;
import com.mvp.vueseum.exception.TourLimitExceededException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
    private HttpServletRequest httpRequest;

    @InjectMocks
    private TourService tourService;

    private TourGenerationRequest request;
    private String deviceFingerprint;
    private List<Artwork> testArtworks;
    private Museum testMuseum;

    @BeforeEach
    void setUp() {
        deviceFingerprint = "test-fingerprint";
        testMuseum = new Museum();
        testMuseum.setId(1L);
        testArtworks = IntStream.range(0, 3)
                .mapToObj(i -> {
                    Artwork artwork = new Artwork();
                    artwork.setId((long) i);
                    artwork.setMuseum(testMuseum);
                    return artwork;
                })
                .collect(Collectors.toList());

        request = TourGenerationRequest.builder()
                .visitorId("test-visitor")
                .museum(testMuseum)
                .preferences(TourPreferences.builder()
                        .museumId(1L)
                        .theme(Tour.TourTheme.CHRONOLOGICAL)
                        .maxStops(5)
                        .minStops(3)
                        .build())
                .build();

    }

    @Test
    void whenGeneratingTour_thenSuccessful() {
        when(artworkService.findArtworkCandidates(any()))
                .thenReturn(testArtworks);
        when(scoringService.scoreArtwork(any(), any(), any()))
                .thenReturn(1.0);
        when(descriptionService.generateTourDescription(any(), any()))
                .thenReturn("Test description");
        when(visitorTrackingService.recordTourGeneration(anyString(), anyString()))
                .thenReturn(true);
        when(deviceFingerprintService.generateFingerprint(any()))
                .thenReturn(deviceFingerprint);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);
        when(museumService.isValidMuseum(anyLong()))
                .thenReturn(true);

        Tour result = tourService.generateTour(request, httpRequest);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(tour -> {
                    assertThat(tour.getStops()).hasSize(3);
                    assertThat(tour.getDeviceFingerprint())
                            .isEqualTo(deviceFingerprint);
                    assertThat(tour.getTheme())
                            .isEqualTo(Tour.TourTheme.CHRONOLOGICAL);
                });

        verify(progressListener).initializeProgress(anyString(), eq("test-visitor"));
        verify(progressListener, atLeastOnce())
                .updateProgress(anyString(), anyDouble(), anyString());
    }

    @Test
    void whenDailyLimitReached_thenThrowsException() {
        // Arrange
        when(visitorTrackingService.recordTourGeneration(anyString(), anyString()))
                .thenReturn(false);
        when(deviceFingerprintService.generateFingerprint(any()))
                .thenReturn(deviceFingerprint);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);
        when(museumService.isValidMuseum(anyLong()))
                .thenReturn(true);

        assertThatThrownBy(() -> tourService.generateTour(request, httpRequest))
                .isInstanceOf(GenerationLimitExceededException.class)
                .hasMessageContaining("Daily tour generation limit");
    }

    @Test
    void whenDeviceLimitReached_thenThrowsException() {
        String fingerprint = "test-fingerprint";
        when(museumService.isValidMuseum(anyLong())).thenReturn(true);
        when(deviceFingerprintService.generateFingerprint(any())).thenReturn(fingerprint);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(fingerprint))
                .thenReturn(10L);

        assertThatThrownBy(() -> tourService.generateTour(request, httpRequest))
                .isInstanceOf(TourLimitExceededException.class)
                .hasMessageContaining("Maximum tour limit");
    }

    @Test
    void whenRequiredArtworksProvided_thenIncludedInTour() {
        request.getPreferences().setRequiredArtworkIds(
                Set.of(testArtworks.getFirst().getId())
        );
        when(artworkService.findArtworkCandidates(any()))
                .thenReturn(testArtworks);
        when(scoringService.scoreArtwork(any(), any(), any()))
                .thenReturn(1.0);
        when(visitorTrackingService.recordTourGeneration(anyString(), anyString()))
                .thenReturn(true);
        when(deviceFingerprintService.generateFingerprint(any()))
                .thenReturn(deviceFingerprint);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);
        when(museumService.isValidMuseum(anyLong()))
                .thenReturn(true);

        Tour result = tourService.generateTour(request, httpRequest);

        assertThat(result.getStops())
                .extracting(stop -> stop.getArtwork().getId())
                .contains(testArtworks.getFirst().getId());
    }

    @Test
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

        TourUpdateRequest updateRequest = new TourUpdateRequest("New Name", "New Description");
        Optional<Tour> result = tourService.updateTourDetails(1L, updateRequest);

        assertThat(result)
                .isPresent()
                .hasValueSatisfying(tour -> {
                    assertThat(tour.getName()).isEqualTo("New Name");
                    assertThat(tour.getDescription()).isEqualTo("New Description");
                    // Verify immutable fields haven't changed
                    assertThat(tour.getTheme()).isEqualTo(Tour.TourTheme.CHRONOLOGICAL);
                    assertThat(tour.getMuseum()).isEqualTo(testMuseum);
                });
    }

    @Test
    void whenDeletingTour_thenMarkedAsDeleted() {
        Tour existingTour = new Tour();
        existingTour.setName("Tour to Delete");

        when(tourRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(existingTour));
        when(tourRepository.save(any(Tour.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tourService.deleteTour(1L);

        verify(tourRepository).save(argThat(baseEntity::getDeleted));
    }

    @Test
    void whenGeneratingDescription_thenUsesCaching() {
        when(artworkService.findArtworkCandidates(any()))
                .thenReturn(testArtworks);
        when(scoringService.scoreArtwork(any(), any(), any()))
                .thenReturn(1.0);
        when(visitorTrackingService.recordTourGeneration(anyString(), anyString()))
                .thenReturn(true);
        when(descriptionService.generateTourDescription(any(), any()))
                .thenReturn("Test description");
        when(deviceFingerprintService.generateFingerprint(any()))
                .thenReturn(deviceFingerprint);
        when(tourRepository.countByDeviceFingerprintAndDeletedFalse(anyString()))
                .thenReturn(0L);
        when(museumService.isValidMuseum(anyLong()))
                .thenReturn(true);

        Tour firstResult = tourService.generateTour(request, httpRequest);
        Tour secondResult = tourService.generateTour(request, httpRequest);

        verify(descriptionService, times(1))
                .generateTourDescription(any(), any());
        assertThat(secondResult.getDescription())
                .isEqualTo(firstResult.getDescription());
    }

    @Test
    void whenInvalidPreferences_thenThrowsException() {
        request.getPreferences().setMaxStops(2);
        when(museumService.isValidMuseum(anyLong()))
                .thenReturn(true);

        assertThatThrownBy(() -> tourService.generateTour(request, httpRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Maximum stops cannot be less than minimum stops");
    }

    @Test
    void whenInvalidMuseum_thenThrowsException() {
        when(museumService.isValidMuseum(anyLong()))
                .thenReturn(false);

        assertThatThrownBy(() -> tourService.generateTour(request, httpRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid museum ID");
    }
}