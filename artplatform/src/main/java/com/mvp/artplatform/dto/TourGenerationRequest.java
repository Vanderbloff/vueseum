package com.mvp.artplatform.dto;

import com.mvp.artplatform.entity.Museum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class TourGenerationRequest {
    private String visitorId;
    private Museum museum;
    private TourPreferences preferences;
}
