package com.mvp.vueseum.service.museum;

import com.mvp.vueseum.config.MuseumConfig;
import com.mvp.vueseum.entity.Museum;
import com.mvp.vueseum.repository.MuseumRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MuseumService {

    private final MuseumRepository museumRepository;
    @SuppressWarnings("FieldMayBeFinal")
    private MuseumConfig museumConfig;

    public MuseumService(MuseumRepository museumRepository, MuseumConfig museumConfig) {
        this.museumRepository = museumRepository;
        this.museumConfig = museumConfig;
    }

    public List<Museum> findAllMuseums() {
        return museumRepository.findAll();
    }

    public Optional<Museum> findMuseumById(Long id) {
        return museumRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean isValidMuseum(Long museumId) {
        return museumRepository.existsById(museumId);
    }

    public Museum findOrCreateMuseum(String apiSource) {
        return museumRepository.findByName(apiSource)
                .orElseGet(() -> {
                    Museum newMuseum = new Museum();
                    String museumIdentifier = determineMuseumIdentifier(apiSource);
                    createMuseumFromProperties(museumIdentifier, newMuseum);
                    // Set other basic museum details
                    // You can enhance these later when you have more information
                    return museumRepository.save(newMuseum);
                });
    }

    private String determineMuseumIdentifier(String museumName) {
        return switch (museumName.toLowerCase()) {
            case "the met", "metropolitan museum of art" -> "metropolitan";
            default -> throw new IllegalStateException("Unknown museum: " + museumName.toLowerCase());
        };
    }

    private void createMuseumFromProperties(String museumIdentifier, Museum museum) {
        MuseumConfig.MuseumProperties properties = museumConfig.setupMuseumConfig(museumIdentifier);

        museum.setName(properties.getName());
        museum.setWebsiteUrl(properties.getWebsiteUrl());
        museum.setLocation(properties.getLocation());
        museum.setMuseumHours(properties.getMuseumHours());
    }
}
