package umc.catchy.domain.mapping.placeCourse.dto.response;

import umc.catchy.domain.mapping.placeCourse.dto.query.PlacePreviewDto;
import umc.catchy.domain.place.domain.Place;

public record PlacePreviewResponse(
        Long placeId,
        String placeName,
        String placeImage,
        String category,
        String roadAddress,
        String activeTime,
        Double rating,
        Double placeLatitude,
        Double placeLongitude,
        Long reviewCount,
        boolean isLiked
) {
    public static PlacePreviewResponse from(PlacePreviewDto dto) {
        return new PlacePreviewResponse(
                dto.getPlaceId(),
                dto.getPlaceName(),
                dto.getPlaceImage(),
                dto.getCategory(),
                dto.getRoadAddress(),
                dto.getActiveTime(),
                dto.getRating(),
                dto.getPlaceLatitude(),
                dto.getPlaceLongitude(),
                dto.getReviewCount(),
                dto.isLiked()
        );
    }

    public static PlacePreviewResponse from(Place place, Long reviewCount, Boolean isLiked) {
        String categoryName = place.getCategory() != null
                ? place.getCategory().getBigCategory().getValue()
                : null;

        Double rating = place.getRating() != null ? place.getRating() : 0.0;

        return new PlacePreviewResponse(
                place.getId(),
                place.getPlaceName(),
                place.getImageUrl(),
                categoryName,
                place.getRoadAddress(),
                place.getActiveTime(),
                rating,
                place.getLatitude(),
                place.getLongitude(),
                reviewCount,
                isLiked
        );
    }
}
