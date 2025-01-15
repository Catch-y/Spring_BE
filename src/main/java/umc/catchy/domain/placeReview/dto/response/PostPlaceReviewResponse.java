package umc.catchy.domain.placeReview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PostPlaceReviewResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class newPlaceReviewResponseDTO{
        Long reviewId;
        Integer rating;
        String comment;
        List<placeReviewImageResponseDTO> reviewImages;
        LocalDateTime visitedDate;
        String creatorNickname;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class placeReviewImageResponseDTO{
        Long reviewImageId;
        String imageUrl;
    }
}
