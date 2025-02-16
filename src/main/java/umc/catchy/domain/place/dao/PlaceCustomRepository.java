package umc.catchy.domain.place.dao;

import java.util.Map;
import org.springframework.data.domain.Slice;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoContainRelevance;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreview;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.place.domain.Place;

import java.util.List;

public interface PlaceCustomRepository {
    List<Place> findPlacesByDynamicFilters(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions);
    List<Place> findRecommendedPlaces(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions, Long memberId, int maxPlaces);
    Slice<PlaceInfoPreview> recommendPlacesByActivityData(Long memberId, Double latitude, Double longitude, List<Long> categoryIds, Map<Long, Integer> hourMap, int pageSize, int page);
    Slice<Place> getPlacesByCategoryWithPaging(BigCategory bigCategory, String groupLocation, String alternativeLocation, int pageSize, Long lastPlaceId, Long groupId);
    List<GptCourseInfoResponse.GptPlaceInfoResponse> findPlacesWithCategoryAndReviewCount(List<Long> placeIds);
    Slice<PlaceInfoContainRelevance> searchPlace(int pageSize, String keyword, Integer lastRelevanceScore, Long lastPlaceId);
}
