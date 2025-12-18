package umc.catchy.domain.mapping.placeCourse.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceDto;

public interface PlaceCourseRepositoryCustom {
    Slice<PlaceDto> searchPlaceByLiked(Long memberId, int pageSize, Long lastPlaceId);
}
