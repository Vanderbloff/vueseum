import com.mvp.vueseum.client.MuseumApiClient;
import com.mvp.vueseum.event.SyncOperation;
import com.mvp.vueseum.exception.ApiClientException;
import com.mvp.vueseum.exception.ResourceNotFoundException;
import com.mvp.vueseum.repository.ArtworkRepository;
import com.mvp.vueseum.service.SyncManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncManagementServiceTest {
    @Mock
    private ArtworkRepository artworkRepository;
    @Mock
    private MuseumApiClient metMuseumClient;
    @Mock
    private MuseumApiClient otherMuseumClient;

    private SyncManagementService syncManagementService;
    private final LocalDateTime testStartTime = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        // Setup museum client IDs
        lenient().when(metMuseumClient.getMuseumId()).thenReturn(1L);
        lenient().when(otherMuseumClient.getMuseumId()).thenReturn(2L);

        // Create list of test clients
        List<MuseumApiClient> clients = Arrays.asList(metMuseumClient, otherMuseumClient);

        // Initialize service with test clients
        syncManagementService = new SyncManagementService(clients, artworkRepository);

        // Setup common mock responses
        lenient().when(metMuseumClient.getSyncStartTime()).thenReturn(testStartTime);
        lenient().when(otherMuseumClient.getSyncStartTime()).thenReturn(testStartTime);
    }

    @Test
    @DisplayName("when museum not found, throws exception")
    void whenMuseumNotFound_throwsException() {
        assertThatThrownBy(() ->
                syncManagementService.executeSync(999L, SyncOperation.daily())
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No API client found for museum ID: 999");
    }

    @Test
    @DisplayName("when performing full sync for specific museum, syncs only that museum")
    void whenPerformingFullSyncForSpecificMuseum_syncsOnlyThatMuseum() {
        SyncOperation operation = SyncOperation.monthly();

        syncManagementService.executeSync(1L, operation);

        verify(metMuseumClient).performSync(operation);
        verify(otherMuseumClient, never()).performSync(any());
    }

    @Test
    @DisplayName("when performing full sync for all museums, syncs all museums")
    void whenPerformingFullSyncForAllMuseums_syncsAllMuseums() {
        SyncOperation operation = SyncOperation.monthly();

        syncManagementService.executeSync(null, operation);

        verify(metMuseumClient).performSync(operation);
        verify(otherMuseumClient).performSync(operation);
    }

    @Test
    @DisplayName("when performing incremental sync, should use daily operation")
    void whenPerformingIncrementalSync_shouldUseDailyOperation() {
        SyncOperation operation = SyncOperation.daily();

        syncManagementService.executeSync(null, operation);

        verify(metMuseumClient).performSync(argThat(op ->
                !op.isFullSync() && op.getIncrementalSince() != null
        ));
        verify(otherMuseumClient).performSync(argThat(op ->
                !op.isFullSync() && op.getIncrementalSince() != null
        ));
    }

    @Test
    @DisplayName("when sync fails for specific museum, should throw exception")
    void whenSyncFailsForSpecificMuseum_shouldThrowException() {
        doThrow(new ApiClientException("Sync failed"))
                .when(metMuseumClient)
                .performSync(any(SyncOperation.class));

        assertThatThrownBy(() ->
                syncManagementService.executeSync(1L, SyncOperation.daily())
        )
                .isInstanceOf(ApiClientException.class)
                .hasMessage("Sync failed");
    }

    @Test
    @DisplayName("when sync fails for one museum, should continue with others")
    void whenSyncFailsForOneMuseum_shouldContinueWithOthers() {
        doThrow(new ApiClientException("Sync failed"))
                .when(metMuseumClient)
                .performSync(any(SyncOperation.class));

        syncManagementService.executeSync(null, SyncOperation.daily());

        verify(metMuseumClient).performSync(any(SyncOperation.class));
        verify(otherMuseumClient).performSync(any(SyncOperation.class));
    }

    @Test
    @DisplayName("when getting sync status for specific museum, returns correct status")
    void whenGettingSyncStatusForSpecificMuseum_returnsCorrectStatus() {
        when(metMuseumClient.getProcessedCount()).thenReturn(100);
        when(metMuseumClient.getErrorCount()).thenReturn(5);
        when(artworkRepository.countByMuseum(1L)).thenReturn(1000L);

        Map<String, Object> status = syncManagementService.getSingleMuseumStatus(1L);

        assertThat(status)
                .containsEntry("museumId", 1L)
                .containsEntry("artworkCount", 1000L)
                .containsEntry("processingErrors", 5)
                .containsEntry("processedCount", 100)
                .containsEntry("syncStartTime", testStartTime);
    }

    @Test
    @DisplayName("when getting aggregated status, returns all museums statuses")
    void whenGettingAggregatedStatus_returnsAllMuseumsStatuses() {
        when(artworkRepository.count()).thenReturn(1500L);
        when(metMuseumClient.getProcessedCount()).thenReturn(100);
        when(otherMuseumClient.getProcessedCount()).thenReturn(200);
        when(metMuseumClient.getErrorCount()).thenReturn(0);
        when(otherMuseumClient.getErrorCount()).thenReturn(0);
        when(artworkRepository.countByMuseum(1L)).thenReturn(1000L);
        when(artworkRepository.countByMuseum(2L)).thenReturn(500L);

        Map<String, Object> status = syncManagementService.getAggregatedStatus();
        
        assertThat(status)
                .containsEntry("totalDisplayedArtworks", 1500L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> museumStatuses =
                (List<Map<String, Object>>) status.get("museumStatuses");

        assertThat(museumStatuses)
                .hasSize(2)
                .allSatisfy(museumStatus ->
                        assertThat(museumStatus).containsKeys(
                                "museumId", "artworkCount", "processingErrors",
                                "syncStartTime", "processedCount"
                        )
                );
    }
}