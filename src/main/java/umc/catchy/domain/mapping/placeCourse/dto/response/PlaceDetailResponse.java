package umc.catchy.domain.mapping.placeCourse.dto.response;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.place.domain.Place;

public record PlaceDetailResponse(
        Long placeId,
        String imageUrl,
        String placeName,
        String placeDescription,
        String categoryName,
        String roadAddress,
        String activeTime,
        String placeSite,
        Double rating,
        Long reviewCount,
        Double placeLatitude,
        Double placeLongitude,
        boolean isVisited,
        boolean liked
) {
    public static PlaceDetailResponse from(Place place, Long reviewCount, Boolean isVisited, Boolean isLiked) {
        BigCategory categoryBigCategory = place.getCategory() != null
                ? place.getCategory().getBigCategory()
                : null;

        Double rating = place.getRating() != null ? place.getRating() : 0.0;

        String categoryName = categoryBigCategory != null ? categoryBigCategory.toString() : null;

        return new PlaceDetailResponse(
                place.getId(),
                place.getImageUrl(),
                place.getPlaceName(),
                place.getPlaceDescription(),
                categoryName,
                place.getRoadAddress(),
                place.getActiveTime(),
                place.getPlaceSite(),
                rating,
                reviewCount,
                place.getLatitude(),
                place.getLongitude(),
                isVisited,
                isLiked
        );
    }
}
