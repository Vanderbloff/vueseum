package com.mvp.vueseum.dto;

import com.mvp.vueseum.entity.Museum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MuseumDTO {
    private Long id;
    private String name;
    private String location;

    public static MuseumDTO fromEntity(Museum museum) {
        MuseumDTO dto = new MuseumDTO();
        dto.id = museum.getId();
        dto.name = museum.getName();
        dto.location = museum.getLocation();
        return dto;
    }
}
