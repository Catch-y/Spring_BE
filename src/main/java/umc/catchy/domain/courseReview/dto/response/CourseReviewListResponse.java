package umc.catchy.domain.courseReview.dto.response;

import java.util.List;

public record CourseReviewListResponse(
        Double courseRating,
        Integer totalCount,
        List<CourseReviewResponse> content,
        Boolean last
) {
}