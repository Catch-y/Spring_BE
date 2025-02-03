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
        String comment;
        Integer rating;
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


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class placeReviewRatingResponseDTO{
        Integer score;
        Long count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class placeReviewAllResponseDTO{
        Float averageRating;
        List<placeReviewRatingResponseDTO> ratingList;
        Long totalCount;
        List<PostPlaceReviewResponse.newPlaceReviewResponseDTO> content;
        Boolean last;
    }
}
