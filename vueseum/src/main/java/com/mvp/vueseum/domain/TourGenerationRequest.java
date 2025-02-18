package com.mvp.vueseum.domain;

import com.mvp.vueseum.entity.Museum;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class TourGenerationRequest {
    private String visitorId;
    private TourPreferences preferences;
}
