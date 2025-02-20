package com.mvp.vueseum.service.artist;

import com.mvp.vueseum.domain.ArtworkDetails;
import com.mvp.vueseum.entity.Artist;
import com.mvp.vueseum.exception.PersistenceException;
import com.mvp.vueseum.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;

    private static final Pattern VALID_YEAR = Pattern.compile("^\\d{4}$");

    public Artist findOrCreateArtist(ArtworkDetails details) {
        try {
            return artistRepository.findByArtistName(details.getArtistName())
                    .orElseGet(() -> {
                        Artist newArtist = new Artist();
                        newArtist.setArtistName(details.getArtistName());
                        System.out.println("newArtist: " + newArtist.getArtistName());

                        // Process dates - must be empty if not valid
                        String birthYear = details.getArtistBirthYear();
                        newArtist.setBirthDate(
                                birthYear != null && VALID_YEAR.matcher(birthYear).matches()
                                        ? birthYear : ""
                        );
                        System.out.println("birthDate: " + newArtist.getBirthDate());


                        String deathYear = details.getArtistDeathYear();
                        newArtist.setDeathDate(
                                deathYear != null && VALID_YEAR.matcher(deathYear).matches()
                                        ? deathYear : ""
                        );
                        System.out.println("deathDate: " + newArtist.getDeathDate());

                        return artistRepository.save(newArtist);
                    });
        } catch (Exception e) {
            throw new PersistenceException("Could not save artist: " + e.getMessage(), e);
        }
    }

    /*private String trimOrNull(String value) {
        return value != null ? value.trim() : null;
    }*/
}
