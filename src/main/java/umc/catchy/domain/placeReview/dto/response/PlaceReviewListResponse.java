package umc.catchy.domain.placeReview.dto.response;

import java.util.List;

public record PlaceReviewListResponse(
        Float averageRating,
        List<PlaceReviewRatingResponse> ratingList,
        Long totalCount,
        List<PlaceReviewResponse> content,
        Boolean last
) {
}