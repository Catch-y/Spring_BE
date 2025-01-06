package umc.catchy.domain.category.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BigCategory {
    CAFE("CAFE"),
    ALCOHOL("ALCOHOL"),
    RESTAURANT("RESTAURANT"),
    EXPERIENCE("EXPERIENCE"),
    CULTURE("CULTURE"),
    SPORTS("SPORTS"),
    WELLNESS("WELLNESS");

    private final String value;
}
