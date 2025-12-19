package umc.catchy.domain.place.dto.response;

import java.util.List;
import java.util.Map;

public record RecommendationContext(
        List<Long> sortedCategories,
        Map<Long, Integer> averageHours
) {
}