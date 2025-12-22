package umc.catchy.domain.courseReview.dto.response;

import umc.catchy.domain.courseReview.domain.CourseReview;
import java.time.LocalDate;
import java.util.List;

public record CourseReviewResponse(
        Long reviewId,
        String comment,
        List<CourseReviewImageResponse> reviewImages,
        LocalDate createdAt,
        String creatorNickname
) {
    public static CourseReviewResponse from(
            CourseReview review,
            List<CourseReviewImageResponse> images
    ) {
        return new CourseReviewResponse(
                review.getId(),
                review.getComment(),
                images,
                review.getCreatedAt(),
                review.getMember().getNickname()
        );
    }
}
