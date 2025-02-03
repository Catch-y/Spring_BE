package umc.catchy.domain.place.dao;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.mapping.placeVisit.domain.QPlaceVisit;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.domain.QPlace;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceCustomRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Place> findPlacesByDynamicFilters(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions) {
        QPlace place = QPlace.place;

        JPAQuery<Place> query = queryFactory
                .selectFrom(place)
                .where(
                        place.category.id.in(categoryIds),  // 카테고리 조건
                        upperRegionFilter(place, upperRegions), // 상위 지역 필터
                        lowerRegionFilter(place, lowerRegions)  // 하위 지역 필터
                );

        return query.fetch();
    }

    private BooleanExpression upperRegionFilter(QPlace place, List<String> upperRegions) {
        if (upperRegions == null || upperRegions.isEmpty()) {
            return null;
        }

        BooleanExpression condition = null;
        for (String region : upperRegions) {
            String normalizedRegion = LocationUtils.normalizeLocation(region);
            BooleanExpression regionCondition = place.roadAddress.like("%" + normalizedRegion + "%");
            condition = (condition == null) ? regionCondition : condition.or(regionCondition);
        }
        return condition;
    }

    private BooleanExpression lowerRegionFilter(QPlace place, List<String> lowerRegions) {
        if (lowerRegions == null || lowerRegions.isEmpty()) {
            return null;
        }

        BooleanExpression condition = null;
        for (String region : lowerRegions) {
            String normalizedRegion = LocationUtils.normalizeLocation(region);
            BooleanExpression regionCondition = place.roadAddress.like("%" + normalizedRegion + "%");
            condition = (condition == null) ? regionCondition : condition.or(regionCondition);
        }
        return condition;
    }

    @Override
    public List<Place> findRecommendedPlaces(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions, Long memberId, int maxPlaces) {
        QPlace place = QPlace.place;
        QPlaceVisit placeVisit = QPlaceVisit.placeVisit;

        List<Place> filteredPlaces = findPlacesByDynamicFilters(categoryIds, upperRegions, lowerRegions);

        List<Long> placeIds = filteredPlaces.stream().map(Place::getId).toList();

        NumberExpression<Double> weightExpression = createWeightExpression(placeVisit);

        // 쿼리 생성: 가중치 계산 및 정렬 추가
        JPAQuery<Place> query = queryFactory
                .selectFrom(place)
                .leftJoin(placeVisit).on(place.id.eq(placeVisit.place.id).and(placeVisit.member.id.eq(memberId)))
                .where(place.id.in(placeIds))
                .orderBy(weightExpression.desc())
                .limit(3L * maxPlaces);

        return query.fetch();
    }

    private NumberExpression<Double> createWeightExpression(QPlaceVisit placeVisit) {
        // 기본 가중치 = 1.0
        NumberExpression<Double> baseWeight = com.querydsl.core.types.dsl.Expressions.asNumber(1.0);

        // 좋아요 여부 가중치 = 0.5
        NumberExpression<Double> likedWeight = placeVisit.isLiked.when(true).then(0.5).otherwise(0.0);

        // 방문 여부 가중치 = 0.3
        NumberExpression<Double> visitedWeight = placeVisit.isVisited.when(true).then(0.3).otherwise(0.0);

        // 최종 가중치 계산식
        return baseWeight.add(likedWeight).add(visitedWeight);
    }
}