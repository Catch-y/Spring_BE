package umc.catchy.domain.mapping.placeLike.dto.response;

import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceResponse;

import java.util.List;

public record LikedPlaceSliceResponse(
        List<PlaceResponse> content,
        Boolean last
) {
    public static LikedPlaceSliceResponse of(List<PlaceResponse> content, Boolean last) {
        return new LikedPlaceSliceResponse(content, last);
    }
}
