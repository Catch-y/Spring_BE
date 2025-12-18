package umc.catchy.domain.course.dto.response;

import umc.catchy.domain.category.domain.BigCategory;

public record GptPlaceInfoDto(
        Long placeId,
        String placeName,
        String placeImage,
        String categoryKey,
        String roadAddress,
        String activeTime,
        double rating,
        int reviewCount
) {
    public GptPlaceInfoResponse toResponse() {
        return new GptPlaceInfoResponse(
                placeId,
                placeName,
                placeImage,
                BigCategory.valueOf(categoryKey).getValue(),
                roadAddress,
                activeTime,
                rating,
                reviewCount
        );
    }
}
