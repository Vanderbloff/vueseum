package com.mvp.artplatform.service.artist;

import com.mvp.artplatform.dto.ArtworkDetails;
import com.mvp.artplatform.exception.PersistenceException;
import com.mvp.artplatform.model.Artist;
import com.mvp.artplatform.repository.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;

    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    public Artist findOrCreateArtist(ArtworkDetails details) {
        try {
            return artistRepository.findByArtistName(details.getArtistName())
                    .orElseGet(() -> {
                        Artist newArtist = new Artist();
                        newArtist.setArtistName(details.getArtistName());
                        newArtist.setNationality(details.getArtistNationality());
                        newArtist.setBirthDate(details.getArtistBirthYear());
                        newArtist.setDeathDate(details.getArtistDeathYear());
                        return artistRepository.save(newArtist);
                    });
        }
        catch (PersistenceException e) {
            throw new PersistenceException("Could not retrieve or create artist within database: " +
                    e.getMessage());
        }
    }
}
