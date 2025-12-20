package umc.catchy.domain.place.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.dto.query.GptPlaceInfoDto;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlacePreviewDto;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceSearchDto;
import umc.catchy.domain.place.domain.Place;

import java.util.List;
import java.util.Map;

public interface PlaceCustomRepository {
    List<Place> findRecommendedPlaces(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions, Long memberId, int maxPlaces);
    Slice<PlacePreviewDto> recommendPlacesByActivityData(Long memberId, Double latitude, Double longitude, List<Long> categoryIds, Map<Long, Integer> hourMap, int pageSize, int page);
    Slice<Place> getPlacesByCategoryWithPaging(BigCategory bigCategory, String groupLocation, String alternativeLocation, int pageSize, Long lastPlaceId, Long groupId);
    List<GptPlaceInfoDto> findPlacesWithCategoryAndReviewCount(List<Long> placeIds);
    Slice<PlaceSearchDto> searchPlace(int pageSize, String keyword, Integer lastRelevanceScore, Long lastPlaceId);
}
