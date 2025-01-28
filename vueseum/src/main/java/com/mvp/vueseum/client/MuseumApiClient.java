package com.mvp.vueseum.client;

import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.event.SyncOperation;
import com.mvp.vueseum.exception.ApiClientException;

import java.time.LocalDateTime;
import java.util.List;

public interface MuseumApiClient {
    List<String> getCurrentlyDisplayedArtworkIds() throws ApiClientException;
    ArtworkDetails fetchArtworkById(String Id);
    int getErrorCount();
    LocalDateTime getSyncStartTime();
    int getProcessedCount();
    Long getMuseumId();

    void performSync(SyncOperation operation);
}
