package com.mvp.vueseum.client;

import com.mvp.vueseum.domain.ArtworkDetails;

public interface MuseumApiClient {
    ArtworkDetails fetchArtworkById(String Id);
    void syncArtworks();
}
