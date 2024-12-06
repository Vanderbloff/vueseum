package com.mvp.artplatform.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.artplatform.model.Museum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "museum")
@PropertySource("classpath:museums.properties")
@Getter
@Setter
public class MuseumConfig {
    private final Environment environment;

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
        //private String address;
        private String websiteUrl;
        private Map<String, Museum.museumHours> museumHours;
        private int rateLimit;
    }

    public MuseumProperties setupMuseumConfig(String museumIdentifier) {
        String basePath = "museum." + museumIdentifier;

        MuseumProperties museumProperties = new MuseumProperties();
        museumProperties.setName(environment.getProperty(basePath + ".name"));
        museumProperties.setLocation(environment.getProperty(basePath + ".location"));
        museumProperties.setWebsiteUrl(environment.getProperty(basePath + ".websiteUrl"));
        museumProperties.setRateLimit(Integer.parseInt(environment.getProperty(basePath + ".api.rateLimit")));
        String hours = environment.getProperty(basePath + ".hours");
        if (hours != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<Map<String, Museum.museumHours>> typeRef =
                        new TypeReference<>() {};
                museumProperties.setMuseumHours(mapper.readValue(hours, typeRef));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing hours for " + museumIdentifier, e);
            }
        }

        return museumProperties;
    }
}
