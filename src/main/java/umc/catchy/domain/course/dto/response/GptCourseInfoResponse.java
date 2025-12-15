package umc.catchy.domain.course.dto.response;

import java.util.List;

public record GptCourseInfoResponse(
        String courseName,
        String courseDescription,
        String recommendTime,
        List<GptPlaceInfoResponse> placeInfos
) {
}
