package umc.catchy.domain.courseReview.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record CourseReviewSliceResponse(
    @Schema(description = "리뷰 데이터") List<PostCourseReviewResponse.newCourseReviewResponseDTO> content,
    @Schema(description = "마지막 페이지 여부") Boolean last){

        public static CourseReviewSliceResponse from(Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> newCourseReviews) {
            return new CourseReviewSliceResponse(newCourseReviews.getContent(),newCourseReviews.isLast());
        }
}
