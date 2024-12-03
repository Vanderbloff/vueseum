package com.mvp.artplatform.client;

import com.mvp.artplatform.model.Artwork;

import java.util.List;

public interface MuseumApiClient {
    List<Artwork> fetchArtworks();
    List<Artwork> fetchArtworksByArtist(String artistName);
    Artwork fetchArtworkDetails(Integer artworkId);
    Artwork convertToArtwork(String body);
}
