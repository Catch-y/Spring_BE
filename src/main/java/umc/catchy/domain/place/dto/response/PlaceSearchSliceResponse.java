package umc.catchy.domain.place.dto.response;

import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceSearchResponse;

import java.util.List;

public record PlaceSearchSliceResponse(
        List<PlaceSearchResponse> content,
        Boolean last
) {
    public static PlaceSearchSliceResponse of(List<PlaceSearchResponse> content, Boolean last) {
        return new PlaceSearchSliceResponse(content, last);
    }
}
