package com.mvp.artplatform.service;

import com.mvp.artplatform.client.museum_client.MetMuseumApiClient;
import com.mvp.artplatform.model.Artwork;
import com.mvp.artplatform.repository.ArtworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtworkAggregationService {
    //private final MetMuseumApiClient metMuseumApiClient;
    private final ArtworkRepository artworkRepository;

    @Autowired
    public ArtworkAggregationService(ArtworkRepository artworkRepository) {
        //this.metMuseumApiClient = new MetMuseumApiClient("https://collectionapi.metmuseum.org/public/collection/v1/objects");
        this.artworkRepository = artworkRepository;
    }

    private void aggregateMuseumData() {
        //List<Artwork> metMuseumArtworks = metMuseumApiClient.fetchArtworks();
    }

    public List<Artwork> getAllArtworks() {
        return artworkRepository.findAll();
    }
}
