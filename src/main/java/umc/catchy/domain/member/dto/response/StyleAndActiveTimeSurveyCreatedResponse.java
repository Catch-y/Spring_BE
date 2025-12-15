package umc.catchy.domain.member.dto.response;

import java.util.List;

public record StyleAndActiveTimeSurveyCreatedResponse(
        List<Long> memberStyleSurveyId,
        List<Long> activeTimeSurveyId
) {
}