package umc.catchy.domain.placeReview.dto.response;

public record PlaceReviewRatingResponse(
        Integer score,
        Long count
) {
}