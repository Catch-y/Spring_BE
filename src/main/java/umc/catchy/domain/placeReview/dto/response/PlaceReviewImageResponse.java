package umc.catchy.domain.placeReview.dto.response;

import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;

public record PlaceReviewImageResponse(
        Long reviewImageId,
        String imageUrl
) {
    public static PlaceReviewImageResponse from(PlaceReviewImage image) {
        return new PlaceReviewImageResponse(
                image.getId(),
                image.getImageUrl()
        );
    }
}
