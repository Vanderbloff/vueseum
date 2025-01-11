package com.mvp.vueseum.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.vueseum.entity.Museum;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@ConfigurationProperties(prefix = "museum")
@PropertySource("classpath:museum.properties")
@Getter
@Setter
public class MuseumConfig {
    private Environment environment;

    @Autowired
    public MuseumConfig(Environment environment) {
        this.environment = environment;
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    @Getter
    @Setter
    public class MuseumProperties {
        private String name;
        private String location;
        private String websiteUrl;

        @Getter(AccessLevel.NONE)
        private Map<String, Museum.MuseumHours> museumHours = new HashMap<>();
        private int rateLimit;

        public Map<String, Museum.MuseumHours> getMuseumHours() {
            return new HashMap<>(museumHours);
        }
    }

    public MuseumProperties setupMuseumConfig(String museumIdentifier) {
        String basePath = "museum." + museumIdentifier;

        MuseumProperties museumProperties = new MuseumProperties();
        museumProperties.setName(environment.getProperty(basePath + ".name"));
        museumProperties.setLocation(environment.getProperty(basePath + ".location"));
        museumProperties.setWebsiteUrl(environment.getProperty(basePath + ".websiteUrl"));
        museumProperties.setRateLimit(Integer.parseInt(Objects.requireNonNull(environment.getProperty(basePath + ".api.rateLimit"))));
        String hours = environment.getProperty(basePath + ".hours");
        if (hours != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<Map<String, Museum.MuseumHours>> typeRef =
                        new TypeReference<>() {};
                museumProperties.setMuseumHours(mapper.readValue(hours, typeRef));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing hours for " + museumIdentifier, e);
            }
        }
        return museumProperties;
    }
}
