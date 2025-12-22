package umc.catchy.domain.placeReview.dto.response;

import umc.catchy.domain.placeReview.domain.PlaceReview;
import java.time.LocalDate;
import java.util.List;

public record PlaceReviewResponse(
        Long reviewId,
        String comment,
        Integer rating,
        List<PlaceReviewImageResponse> reviewImages,
        LocalDate visitedDate,
        String creatorNickname
) {
    public static PlaceReviewResponse from(
            PlaceReview placeReview,
            List<PlaceReviewImageResponse> images
    ) {
        return new PlaceReviewResponse(
                placeReview.getId(),
                placeReview.getComment(),
                placeReview.getRating(),
                images,
                placeReview.getVisitedDate(),
                placeReview.getMember().getNickname()
        );
    }
}
