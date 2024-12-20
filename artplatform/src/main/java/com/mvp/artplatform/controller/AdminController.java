package com.mvp.artplatform.controller;

import com.mvp.artplatform.client.museum_client.MetMuseumApiClient;
import com.mvp.artplatform.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final MetMuseumApiClient metMuseumApiClient;
    private final ArtworkRepository artworkRepository;

    @PostMapping("/sync/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startSync() {
        metMuseumApiClient.syncArtworks();
    }

    @GetMapping("/sync/status")
    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalArtworksInDb", artworkRepository.count());
        status.put("processingErrors", metMuseumApiClient.getErrorCount());
        status.put("syncStartTime", metMuseumApiClient.getSyncStartTime());
        status.put("processedCount", metMuseumApiClient.getProcessedCount());
        return status;
    }
}
