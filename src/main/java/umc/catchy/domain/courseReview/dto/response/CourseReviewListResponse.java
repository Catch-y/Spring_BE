package umc.catchy.domain.courseReview.dto.response;

import java.util.List;

public record CourseReviewListResponse(
        Double courseRating,
        Integer totalCount,
        List<CourseReviewResponse> content,
        Boolean last
) {
    public static CourseReviewListResponse of(Double courseRating, Integer totalCount, List<CourseReviewResponse> content, Boolean last) {
        return new CourseReviewListResponse(courseRating, totalCount, content, last);
    }
}
