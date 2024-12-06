package com.mvp.artplatform.service.artwork;

import com.mvp.artplatform.dto.ArtworkDetails;
import com.mvp.artplatform.exception.PersistenceException;
import com.mvp.artplatform.exception.InvalidRequestException;
import com.mvp.artplatform.model.Artist;
import com.mvp.artplatform.model.Artwork;
import com.mvp.artplatform.model.Museum;
import com.mvp.artplatform.repository.ArtworkRepository;
import com.mvp.artplatform.service.artist.ArtistService;
import com.mvp.artplatform.service.museum.MuseumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@CacheConfig(cacheNames = "artworks")
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtistService artistService;
    private final MuseumService museumService;

    @Autowired
    public ArtworkService(ArtworkRepository artworkRepository, ArtistService artistService, MuseumService museumService) {
        this.artworkRepository = artworkRepository;
        this.artistService = artistService;
        this.museumService = museumService;
    }

    @Cacheable(key = "#details.externalId")
    public Artwork saveFromDetails(ArtworkDetails details) {
        // Find or create the artist
        try {
            Artist artist = artistService.findOrCreateArtist(details);

            // Find or create the museum
            Museum museum = museumService.findOrCreateMuseum(details.getApiSource());

            // Find or create the artwork
            Artwork artwork = artworkRepository.findByExternalIdAndMuseum(
                    details.getExternalId(),
                    museum
            ).orElseGet(() -> {
                Artwork newArtwork = new Artwork();
                newArtwork.setExternalId(details.getExternalId());
                newArtwork.setMuseum(museum);
                return artworkRepository.save(newArtwork);
            });
            updateArtworkFromDetails(artwork, details, artist);
            return artworkRepository.save(artwork);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("Invalid artwork data: " +
                    e.getMessage());
        }
        catch (PersistenceException e) {
            throw new PersistenceException("Database error while saving artwork: " +
                    e.getMessage());
        }
    }

    private void updateArtworkFromDetails(Artwork artwork, ArtworkDetails details, Artist artist) {
        artwork.setTitle(details.getTitle());
        artwork.setArtist(artist);
        artwork.setMedium(details.getMedium());
        artwork.setImageUrl(details.getPrimaryImageUrl());
        artwork.setDescription(details.getDescription());
        artwork.setCountry(details.getCurrentLocation());
        artwork.setGalleryNumber(details.getGalleryNumber());
        artwork.setCreationDate(String.valueOf(details.getCreationYear()));
        artwork.setIsOnDisplay(details.getIsOnView());

        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("tags", details.getTags());
        additionalMetadata.put("creditLine", details.getCreditLine());
        additionalMetadata.put("additionalImageUrls", details.getAdditionalImageUrls());

        artwork.setAdditionalMetadata(additionalMetadata);
    }
}
