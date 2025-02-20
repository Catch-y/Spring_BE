package umc.catchy.domain.category.domain;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Getter
@AllArgsConstructor
public enum BigCategory {
    CAFE("카페"),
    BAR("주류"),
    RESTAURANT("음식점"),
    EXPERIENCE("체험"),
    CULTURELIFE("문화생활"),
    SPORT("스포츠"),
    REST("휴식");

    private final String value;

    public static BigCategory findByName(String name) {
        return Arrays.stream(BigCategory.values())
                .filter(bigCategory -> bigCategory.getValue().equals(name))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_CATEGORY));
    }
}
