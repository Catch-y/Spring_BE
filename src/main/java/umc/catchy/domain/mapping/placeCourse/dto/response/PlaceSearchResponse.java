package umc.catchy.domain.mapping.placeCourse.dto.response;

import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceSearchDto;

public record PlaceSearchResponse(
        PlaceResponse placeInfoResponse,
        Integer relevanceScore
) {
    public static PlaceSearchResponse from(PlaceSearchDto dto) {
        PlaceResponse placeInfoResponse = new PlaceResponse(
                dto.getPlaceId(),
                dto.getImageUrl(),
                dto.getPlaceName(),
                dto.getCategoryName(),
                dto.getRoadAddress(),
                dto.getActiveTime(),
                dto.getRating(),
                dto.getReviewCount()
        );

        return new PlaceSearchResponse(placeInfoResponse, dto.getRelevanceScore());
    }
}
