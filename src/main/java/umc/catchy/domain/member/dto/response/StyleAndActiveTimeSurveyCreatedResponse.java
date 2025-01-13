package umc.catchy.domain.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StyleAndActiveTimeSurveyCreatedResponse {
    List<Long> memberStyleSurveyId;
    List<Long> activeTimeSurveyId;
}
