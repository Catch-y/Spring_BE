package umc.catchy.domain.mapping.placeCourse.dto.response;

import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceDto;

public record PlaceResponse(
        Long placeId,
        String imageUrl,
        String placeName,
        String categoryName,
        String roadAddress,
        String activeTime,
        Double rating,
        Long reviewCount
) {
    public static PlaceResponse from(PlaceDto dto) {
        return new PlaceResponse(
                dto.getPlaceId(),
                dto.getImageUrl(),
                dto.getPlaceName(),
                dto.getCategoryName(),
                dto.getRoadAddress(),
                dto.getActiveTime(),
                dto.getRating(),
                dto.getReviewCount()
        );
    }
}
