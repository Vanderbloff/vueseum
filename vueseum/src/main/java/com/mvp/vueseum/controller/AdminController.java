package com.mvp.vueseum.controller;

import com.mvp.vueseum.event.SyncOperation;
import com.mvp.vueseum.service.SyncManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final SyncManagementService syncService;

    @PostMapping("/sync/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startSync(
            @RequestParam(required = false) Long museumId,
            @RequestParam(defaultValue = "false") boolean fullSync) {
        SyncOperation operation = fullSync
                ? SyncOperation.monthly()  // Full sync
                : SyncOperation.daily();   // Incremental sync

        syncService.startSync(museumId, operation);
    }

    @GetMapping("/sync/status")
    public Map<String, Object> getSyncStatus(@RequestParam(required = false) Long museumId) {
        return museumId != null ?
                syncService.getSingleMuseumStatus(museumId) :
                syncService.getAggregatedStatus();
    }
}
