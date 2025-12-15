package umc.catchy.domain.category.dto.request;

import java.util.List;

public record CategorySurveyRequest(
        List<String> categories
) {
}
