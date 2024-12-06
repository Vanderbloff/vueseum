package com.mvp.artplatform.service.museum;

import com.mvp.artplatform.config.MuseumConfig;
import com.mvp.artplatform.model.Museum;
import com.mvp.artplatform.repository.MuseumRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Museum findOrCreateMuseum(String apiSource) {
        return museumRepository.findMuseumByName(apiSource)
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
