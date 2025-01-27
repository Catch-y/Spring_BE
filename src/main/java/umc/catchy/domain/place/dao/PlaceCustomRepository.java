package umc.catchy.domain.place.dao;

import umc.catchy.domain.place.domain.Place;

import java.util.List;

public interface PlaceCustomRepository {
    List<Place> findPlacesByDynamicFilters(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions);
    List<Place> findRecommendedPlaces(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions, Long memberId, int maxPlaces);
}
