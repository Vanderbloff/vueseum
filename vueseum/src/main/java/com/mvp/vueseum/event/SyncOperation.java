package com.mvp.vueseum.event;

import lombok.Getter;

import java.time.LocalDateTime;

public class SyncOperation {
    private final boolean isFullSync;
    @Getter
    private final LocalDateTime startTime;
    @Getter
    private final LocalDateTime incrementalSince;

    public static SyncOperation daily() {
        return new SyncOperation(false, LocalDateTime.now().minusDays(1));
    }

    public static SyncOperation monthly() {
        return new SyncOperation(true, null);
    }

    private SyncOperation(boolean isFullSync, LocalDateTime incrementalSince) {
        this.isFullSync = isFullSync;
        this.startTime = LocalDateTime.now();
        this.incrementalSince = incrementalSince;
    }

    public boolean isFullSync() {
        return isFullSync;
    }

}
