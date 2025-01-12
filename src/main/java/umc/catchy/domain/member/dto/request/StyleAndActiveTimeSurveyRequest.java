package umc.catchy.domain.member.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import umc.catchy.domain.style.domain.StyleName;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Getter
public class StyleAndActiveTimeSurveyRequest {
    @NotNull(message = "한 개 이상 골라야합니다.")
    List<StyleName> styleNames;
    @NotNull(message = "한 개 이상 골라야합니다.")
    List<DayOfWeek> daysOfWeeks;
    LocalTime startTime;
    LocalTime endTime;
}
