package umc.catchy.domain.category.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BigCategory {
    CAFE("카페"),
    ALCOHOL("주류"),
    RESTAURANT("음식점"),
    EXPERIENCE("체험"),
    CULTURE("문화생활"),
    SPORTS("스포츠"),
    WELLNESS("휴식/웰니스");

    private final String value;
}
