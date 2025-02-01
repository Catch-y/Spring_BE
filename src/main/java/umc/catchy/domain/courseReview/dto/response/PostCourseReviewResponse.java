package umc.catchy.domain.courseReview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
        //LocalDateTime visitedDate;
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
}
