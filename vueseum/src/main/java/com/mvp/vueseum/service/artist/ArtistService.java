package com.mvp.vueseum.service.artist;

import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.exception.PersistenceException;
import com.mvp.vueseum.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;

    public Artist findOrCreateArtist(ArtworkDetails details) {
        String artistName = details.getArtistName() != null ?
                details.getArtistName().trim() : "Unknown Artist";

        try {
            return artistRepository.findByArtistName(artistName)
                    .orElseGet(() -> {
                        Artist newArtist = new Artist();
                        newArtist.setArtistName(artistName);
                        newArtist.setNationality(trimOrNull(details.getArtistNationality()));
                        newArtist.setBirthDate(trimOrNull(details.getArtistBirthYear()));
                        newArtist.setDeathDate(trimOrNull(details.getArtistDeathYear()));
                        return artistRepository.save(newArtist);
                    });
        }
        catch (PersistenceException e) {
            throw new PersistenceException("Could not retrieve or create artist within database: " +
                    e.getMessage());
        }
    }

    private String trimOrNull(String value) {
        return value != null ? value.trim() : null;
    }
}
