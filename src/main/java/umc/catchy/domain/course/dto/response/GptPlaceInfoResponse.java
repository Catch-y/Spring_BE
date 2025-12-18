package umc.catchy.domain.course.dto.response;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.dto.query.GptPlaceInfoDto;

public record GptPlaceInfoResponse(
        Long placeId,
        String placeName,
        String placeImage,
        String category,
        String roadAddress,
        String activeTime,
        double rating,
        int reviewCount
) {
    public static GptPlaceInfoResponse from(GptPlaceInfoDto dto) {
        return new GptPlaceInfoResponse(
                dto.placeId(),
                dto.placeName(),
                dto.placeImage(),
                BigCategory.valueOf(dto.categoryKey()).getValue(),
                dto.roadAddress(),
                dto.activeTime(),
                dto.rating(),
                dto.reviewCount()
        );
    }
}
