package umc.catchy.domain.place.dao;
import static umc.catchy.domain.category.domain.QCategory.category;
import static umc.catchy.domain.mapping.memberPlaceVote.domain.QMemberPlaceVote.memberPlaceVote;
import static umc.catchy.domain.mapping.placeLike.domain.QPlaceLike.placeLike;
import static umc.catchy.domain.mapping.placeVisit.domain.QPlaceVisit.placeVisit;
import static umc.catchy.domain.place.domain.QPlace.place;
import static umc.catchy.domain.placeReview.domain.QPlaceReview.placeReview;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.dto.response.GptCourseInfoResponse;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoContainRelevance;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoPreview;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;
import umc.catchy.domain.mapping.placeLike.domain.QPlaceLike;
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
        QPlaceLike placeLike = QPlaceLike.placeLike;

        List<Place> filteredPlaces = findPlacesByDynamicFilters(categoryIds, upperRegions, lowerRegions);

        List<Long> placeIds = filteredPlaces.stream().map(Place::getId).toList();

        NumberExpression<Double> weightExpression = createWeightExpression(placeVisit);

        // 쿼리 생성: 가중치 계산 및 정렬 추가
        JPAQuery<Place> query = queryFactory
                .selectFrom(place)
                .leftJoin(placeVisit).on(place.id.eq(placeVisit.place.id).and(placeVisit.member.id.eq(memberId)))
                .leftJoin(placeLike).on(place.id.eq(placeLike.place.id).and(placeLike.member.id.eq(memberId)))
                .where(place.id.in(placeIds))
                .orderBy(weightExpression.desc())
                .limit(3L * maxPlaces);

        return query.fetch();
    }

    private NumberExpression<Double> createWeightExpression(QPlaceVisit placeVisit) {
        // 기본 가중치 = 1.0
        NumberExpression<Double> baseWeight = com.querydsl.core.types.dsl.Expressions.asNumber(1.0);

        // 좋아요 여부 가중치 = 0.5
        NumberExpression<Double> likedWeight = placeLike.isLiked.when(true).then(0.5).otherwise(0.0);

        // 방문 여부 가중치 = 0.3
        NumberExpression<Double> visitedWeight = placeVisit.isVisited.when(true).then(0.3).otherwise(0.0);

        // 최종 가중치 계산식
        return baseWeight.add(likedWeight).add(visitedWeight);
    }

    @Override
    public Slice<PlaceInfoPreview> recommendPlacesByActivityData(Long memberId, Double latitude, Double longitude,
                                                                 List<Long> categoryIds,
                                                                 Map<Long, Integer> hourMap,
                                                                 int pageSize, int page) {
        // 모든 카테고리에 대한 데이터를 한 번에 가져옴
        List<PlaceInfoPreview> results = getPlaceInfoPreview(memberId, categoryIds, hourMap, latitude, longitude, pageSize, page - 1);

        // 페이징 처리
        boolean hasNext = results.size() > pageSize;
        if (hasNext) {
            results = results.subList(0, pageSize); // pageSize만큼만 잘라냄
        }

        return new SliceImpl<>(results, PageRequest.of(page, pageSize), hasNext);
    }

    private List<PlaceInfoPreview> getPlaceInfoPreview(Long memberId, List<Long> categoryIds, Map<Long, Integer> hourMap,
                                                       Double userLatitude, Double userLongitude, int pageSize, int page) {

        // 사용자 위치와 장소 거리 계산(가까운 순으로 정렬)
        NumberExpression<Double> distance = Expressions.numberTemplate(Double.class,
                "(6371 * ACOS(COS(RADIANS({0})) * COS(RADIANS({1})) * COS(RADIANS({2}) - RADIANS({3})) + SIN(RADIANS({0})) * SIN(RADIANS({1}))))",
                userLatitude, place.latitude, place.longitude, userLongitude);

        // 카테고리 정렬 순서 설정(사용자가 방문 빈도가 높은 순)
        NumberExpression<Integer> categoryOrder = Expressions.asNumber(categoryIds.size());

        for (int i = 0; i < categoryIds.size(); i++) {
            Long categoryId = categoryIds.get(i);
            NumberExpression<Integer> orderValue = Expressions.asNumber(i);

            categoryOrder = new CaseBuilder()
                    .when(place.category.id.eq(categoryId)).then(orderValue)
                    .otherwise(categoryOrder);
        }

        // 카테고리별 평균 시간 조건을 적용
        List<BooleanExpression> hourConditions = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            Integer avgHour = hourMap.get(categoryId);
            if (avgHour != null) {
                hourConditions.add(hourCondition(avgHour).and(place.category.id.eq(categoryId)));
            }
        }

        // 쿼리 생성
        JPQLQuery<PlaceInfoPreview> query = queryFactory.select(Projections.fields(PlaceInfoPreview.class,
                        place.id.as("placeId"),
                        place.placeName.as("placeName"),
                        place.imageUrl.as("placeImage"),
                        place.category.name.as("category"),
                        place.roadAddress.as("roadAddress"),
                        place.activeTime.as("activeTime"),
                        placeReview.rating.avg().coalesce(0.0).as("rating"),
                        placeReview.count().as("reviewCount"),
                        placeLike.isLiked.as("isLiked")
                ))
                .from(place)
                .leftJoin(placeReview).on(placeReview.place.id.eq(place.id))
                .leftJoin(placeVisit).on(place.id.eq(placeVisit.place.id).and(placeVisit.member.id.eq(memberId)))
                .leftJoin(placeLike).on(place.id.eq(placeLike.place.id).and(placeLike.member.id.eq(memberId)))
                .where(
                        place.category.id.in(categoryIds), // 카테고리 필터링
                        ExpressionUtils.anyOf(hourConditions.toArray(new BooleanExpression[0])), // 시간 조건 적용
                        notContainVisited(memberId) // 이미 방문했던 장소 필터링
                )
                .groupBy(place.id)
                .orderBy(categoryOrder.asc(), distance.asc())
                .offset((long) page * pageSize) // 페이징 offset
                .limit(pageSize + 1); // pageSize + 1로 다음 페이지 존재 여부 확인

        return query.fetch();
    }

    private BooleanExpression hourCondition(Integer avgHour) {
        LocalTime targetTime = LocalTime.of(avgHour, 0);

        return place.startTime.isNull()
                .or(place.endTime.isNull())
                .or(place.startTime.isNotNull()
                        .and(place.endTime.isNotNull())
                        .and(place.startTime.loe(targetTime))
                        .and(place.endTime.gt(targetTime)));
    }

    private BooleanExpression notContainVisited(Long memberId) {
        return place.id.notIn(
                JPAExpressions.select(placeVisit.place.id)
                        .from(placeVisit)
                        .where(placeVisit.member.id.eq(memberId))
        );
    }

    public Slice<Place> getPlacesByCategoryWithPaging(
            BigCategory bigCategory, String groupLocation, String alternativeLocation, int pageSize, Long lastPlaceId, Long groupId) {

        // 마지막 장소 정보 가져오기
        Tuple lastPlaceInfo = null;
        if (lastPlaceId != null) {
            lastPlaceInfo = queryFactory
                    .select(place.id,
                            JPAExpressions
                                    .select(memberPlaceVote.count())
                                    .from(memberPlaceVote)
                                    .where(memberPlaceVote.place.id.eq(place.id)
                                            .and(memberPlaceVote.group.id.eq(groupId))),
                            place.placeName)
                    .from(place)
                    .where(place.id.eq(lastPlaceId))
                    .fetchOne();
        }

        Long lastVoteCount = lastPlaceInfo != null ? lastPlaceInfo.get(1, Long.class) : null;
        String lastPlaceName = lastPlaceInfo != null ? lastPlaceInfo.get(place.placeName) : null;

        NumberExpression<Long> groupVoteCount = Expressions.asNumber(
                JPAExpressions
                        .select(memberPlaceVote.count())
                        .from(memberPlaceVote)
                        .where(memberPlaceVote.place.id.eq(place.id)
                                .and(memberPlaceVote.group.id.eq(groupId)))
        );
        List<Place> places = queryFactory
                .selectFrom(place)
                .leftJoin(memberPlaceVote).on(memberPlaceVote.place.id.eq(place.id))
                .join(place.category, category)
                .where(
                        category.bigCategory.eq(bigCategory),
                        place.roadAddress.like("%" + groupLocation + "%")
                                .or(place.roadAddress.like("%" + alternativeLocation + "%"))
                )
                .groupBy(place.id)
                .having(
                        lastPlaceId == null ? null : (
                                groupVoteCount.lt(lastVoteCount)
                                        .or(groupVoteCount.eq(lastVoteCount)
                                                .and(place.placeName.gt(lastPlaceName)))
                                        .or(groupVoteCount.eq(lastVoteCount)
                                                .and(place.placeName.eq(lastPlaceName))
                                                .and(place.id.gt(lastPlaceId)))
                        )
                )
                .orderBy(
                        groupVoteCount.desc(),
                        place.placeName.asc(),
                        place.id.asc()
                )
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = places.size() > pageSize;
        if (hasNext) {
            places.remove(pageSize);
        }

        return new SliceImpl<>(places, PageRequest.of(0, pageSize), hasNext);
    }

    @Override
    public List<GptCourseInfoResponse.GptPlaceInfoResponse> findPlacesWithCategoryAndReviewCount(List<Long> placeIds) {
        return queryFactory.select(Projections.constructor(
                        GptCourseInfoResponse.GptPlaceInfoResponse.class,
                        place.id,
                        place.placeName,
                        place.imageUrl,
                        category.bigCategory.stringValue(),
                        place.roadAddress,
                        place.activeTime,
                        place.rating.coalesce(0.0),
                        placeReview.count().coalesce(0L).intValue()
                ))
                .from(place)
                .leftJoin(place.category, category)
                .leftJoin(placeReview).on(placeReview.place.id.eq(place.id))
                .where(place.id.in(placeIds))
                .groupBy(place.id, place.placeName, place.imageUrl, category.bigCategory, place.roadAddress, place.activeTime, place.rating)
                .fetch();
    }

    @Override
    public Slice<PlaceInfoContainRelevance> searchPlace(int pageSize, String keyword, Integer lastRelevanceScore, Long lastPlaceId) {

        /*
        NumberExpression<Double> distance = Expressions.numberTemplate(Double.class,
                "(6371 * ACOS(COS(RADIANS({0})) * COS(RADIANS({1})) * COS(RADIANS({2}) - RADIANS({3})) + SIN(RADIANS({0})) * SIN(RADIANS({1}))))",
                latitude, place.latitude, place.longitude, longitude);
         */

        // 키워드가 없으면 바로 빈 리스트 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), false);
        }

        NumberExpression<Integer> relevanceScore = getRelevanceScore(keyword);

        List<PlaceInfoContainRelevance> results = queryFactory.select(
                Projections.fields(PlaceInfoContainRelevance.class,
                        Projections.fields(PlaceInfoResponse.class,
                        place.id.as("placeId"),
                        place.imageUrl.as("imageUrl"),
                        place.placeName.as("placeName"),
                        place.category.bigCategory.stringValue().as("categoryName"),
                        place.roadAddress.as("roadAddress"),
                        place.activeTime.as("activeTime"),
                        placeReview.rating.avg().coalesce(0.0).as("rating"),
                        placeReview.count().as("reviewCount")
                        ).as("placeInfoResponse")
                        ,relevanceScore.as("relevanceScore")))
                .from(place)
                .leftJoin(placeReview).on(placeReview.place.id.eq(place.id))
                .where(
                        keywordContains(keyword),
                        cursorCondition(relevanceScore,lastRelevanceScore, lastPlaceId)
                )
                .groupBy(place.id)
                .orderBy(relevanceScore.desc(),place.id.desc())
                .limit(pageSize + 1)
                .fetch();

        return checkLastPage(pageSize,results);
    }

    private static NumberExpression<Integer> getRelevanceScore(String keyword) {
        NumberExpression<Integer> relevanceScore = Expressions.numberTemplate(Integer.class,
                "CASE " +
                        "WHEN {0} = {1} THEN 100 " + // 완전 일치 (100점)
                        "WHEN {0} LIKE CONCAT('%', {1}, '%') THEN 80 " + // 포함 (80점)
                        "WHEN {2} = {1} THEN 60 " + // 카테고리 완전 일치 (60점)
                        "WHEN {2} LIKE CONCAT('%', {1}, '%') THEN 40 " + // 카테고리 포함 (40점)
                        "ELSE 0 END",
                place.placeName, keyword, place.category.bigCategory.stringValue());
        return relevanceScore;
    }

    private BooleanExpression keywordContains(String keyword) {
        return place.placeName.containsIgnoreCase(keyword).or(place.category.bigCategory.stringValue().containsIgnoreCase(keyword));
    }

    private BooleanExpression cursorCondition(NumberExpression<Integer> relevanceScore,Integer lastRelevanceScore, Long lastPlaceId) {
        if (lastRelevanceScore == null || lastPlaceId == null) {
            return null; // 첫 페이지 요청 시 조건 없음
        }

        // 더 낮은 relevanceScore를 가진 데이터 가져오기
        BooleanExpression lowerScore = relevanceScore.lt(lastRelevanceScore);

        // 같은 relevanceScore에서 placeId가 더 작은 데이터 가져오기 (중복 방지)
        BooleanExpression sameScoreLargerId = relevanceScore.eq(lastRelevanceScore)
                .and(place.id.lt(lastPlaceId));

        return lowerScore.or(sameScoreLargerId);
    }

    private Slice<PlaceInfoContainRelevance> checkLastPage(int pageSize, List<PlaceInfoContainRelevance> results) {
        if (results.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), false);
        }

        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0,pageSize), hasNext);
    }

}