package umc.catchy.domain.courseReview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class PostCourseReviewResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class newCourseReviewResponseDTO{
        Long reviewId;
        String comment;
        List<courseReviewImageResponseDTO> reviewImages;
        LocalDate createdAt;
        String creatorNickname;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class courseReviewImageResponseDTO{
        Long reviewImageId;
        String imageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class courseReviewAllResponseDTO{
        Double courseRating;
        Integer totalCount;
        List<PostCourseReviewResponse.newCourseReviewResponseDTO> content;
        Boolean last;
    }
}
