package umc.catchy.domain.placeReview.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
        LocalDate visitedDate;
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
