package umc.catchy.domain.placeReview.dto.response;

import java.util.List;

public record PlaceReviewListResponse(
        Float averageRating,
        List<PlaceReviewRatingResponse> ratingList,
        Long totalCount,
        List<PlaceReviewResponse> content,
        Boolean last
) {
    public static PlaceReviewListResponse of(Float averageRating, List<PlaceReviewRatingResponse> ratingList, Long totalCount, List<PlaceReviewResponse> content, Boolean last) {
        return new PlaceReviewListResponse(averageRating, ratingList, totalCount, content, last);
    }
}
