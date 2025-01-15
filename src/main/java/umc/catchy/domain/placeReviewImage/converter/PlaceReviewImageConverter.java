package umc.catchy.domain.placeReviewImage.converter;

import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;

public class PlaceReviewImageConverter {

    public static PostPlaceReviewResponse.placeReviewImageResponseDTO toPlaceReviewImageResponseDTO(PlaceReviewImage placeReviewImage){
        return PostPlaceReviewResponse.placeReviewImageResponseDTO.builder()
                .reviewImageId(placeReviewImage.getId())
                .imageUrl(placeReviewImage.getImageUrl())
                .build();
    }

    public static PlaceReviewImage toPlaceReviewImage(PlaceReview placeReview, String url){
        return PlaceReviewImage.builder()
                .imageUrl(url)
                .placeReview(placeReview)
                .build();
    }
}
