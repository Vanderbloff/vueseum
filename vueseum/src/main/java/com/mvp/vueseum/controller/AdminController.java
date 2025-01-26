package com.mvp.vueseum.controller;

import com.mvp.vueseum.service.SyncManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final SyncManagementService syncService;

    @PostMapping("/sync/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startSync(@RequestParam(required = false) Long museumId) {
        syncService.startSync(museumId);
    }

    @GetMapping("/sync/status")
    public Map<String, Object> getSyncStatus(@RequestParam(required = false) Long museumId) {
        return museumId != null ?
                syncService.getSingleMuseumStatus(museumId) :
                syncService.getAggregatedStatus();
    }
}
