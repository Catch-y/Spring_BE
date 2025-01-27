package umc.catchy.domain.place.dao;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.place.domain.QPlace;

import java.util.List;

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
            BooleanExpression regionCondition = place.roadAddress.like(region + "%"); // "서울시%"
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
            BooleanExpression regionCondition = place.roadAddress.like("%" + region + "%"); // "%성북구%"
            condition = (condition == null) ? regionCondition : condition.or(regionCondition);
        }
        return condition;
    }
}
