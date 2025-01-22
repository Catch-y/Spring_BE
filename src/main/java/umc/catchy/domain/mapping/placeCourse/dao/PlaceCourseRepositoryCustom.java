package umc.catchy.domain.mapping.placeCourse.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;

public interface PlaceCourseRepositoryCustom {
    Slice<PlaceInfoResponse> searchPlaceByLiked(Long memberId, int pageSize, Long lastPlaceId);
}
