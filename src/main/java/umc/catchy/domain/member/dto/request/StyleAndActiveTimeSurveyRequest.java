package umc.catchy.domain.member.dto.request;

import lombok.Getter;
import umc.catchy.domain.style.domain.StyleName;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Getter
public class StyleAndActiveTimeSurveyRequest {
    List<StyleName> styleNames;
    List<DayOfWeek> daysOfWeeks;
    LocalTime startTime;
    LocalTime endTime;
}
