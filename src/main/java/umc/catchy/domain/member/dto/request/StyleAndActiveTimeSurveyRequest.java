package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;
import umc.catchy.domain.activetime.dto.ActiveTimeRequest;
import umc.catchy.domain.style.domain.StyleName;

import java.util.List;

public record StyleAndActiveTimeSurveyRequest(
        @NotNull(message = "한 개 이상 골라야합니다.")
        List<StyleName> styleNames,

        @NotNull(message = "한 개 이상 골라야합니다.")
        List<ActiveTimeRequest> activeTimes
) {
}
