package umc.catchy.domain.place.dao;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.dto.query.GptPlaceInfoDto;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlacePreviewDto;
import umc.catchy.domain.mapping.placeCourse.dto.query.PlaceSearchDto;
import umc.catchy.domain.mapping.placeVisit.domain.QPlaceVisit;
import umc.catchy.domain.place.domain.Place;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static umc.catchy.domain.category.domain.QCategory.category;
import static umc.catchy.domain.mapping.memberPlaceVote.domain.QMemberPlaceVote.memberPlaceVote;
import static umc.catchy.domain.mapping.placeLike.domain.QPlaceLike.placeLike;
import static umc.catchy.domain.mapping.placeVisit.domain.QPlaceVisit.placeVisit;
import static umc.catchy.domain.place.domain.QPlace.place;
import static umc.catchy.domain.placeReview.domain.QPlaceReview.placeReview;

@Slf4j
@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Place> findRecommendedPlaces(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions, Long memberId, int maxPlaces) {
        List<Long> placeIds = findPlacesByDynamicFilters(categoryIds, upperRegions, lowerRegions);

        if (placeIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(place)
                .join(place.category, category).fetchJoin()
                .leftJoin(placeVisit).on(place.id.eq(placeVisit.place.id).and(placeVisit.member.id.eq(memberId)))
                .leftJoin(placeLike).on(place.id.eq(placeLike.place.id).and(placeLike.member.id.eq(memberId)))
                .where(place.id.in(placeIds))
                .orderBy(createWeightExpression().desc())
                .limit(3L * maxPlaces)
                .fetch();
    }

    private List<Long> findPlacesByDynamicFilters(List<Long> categoryIds, List<String> upperRegions, List<String> lowerRegions) {
        return queryFactory
                .select(place.id)
                .from(place)
                .where(
                        place.category.id.in(categoryIds),
                        sidoIn(upperRegions),
                        sigunguIn(lowerRegions)
                )
                .fetch();
    }

    private BooleanExpression sidoIn(List<String> upperRegions) {
        if (upperRegions == null || upperRegions.isEmpty()) {
            return null;
        }

        List<String> normalizedUpper = upperRegions.stream()
                .map(LocationUtils::normalizeLocation)
                .toList();

        return place.sido.in(normalizedUpper);
    }

    private BooleanExpression sigunguIn(List<String> lowerRegions) {
        if (lowerRegions == null || lowerRegions.isEmpty()) {
            return null;
        }

        return place.sigungu.in(lowerRegions);
    }

    private NumberExpression<Double> createWeightExpression() {
        // 기본 가중치 = 1.0
        NumberExpression<Double> baseWeight = Expressions.asNumber(1.0);

        // 좋아요 여부 가중치 = 0.5
        NumberExpression<Double> likedWeight = placeLike.isLiked.when(true).then(0.5).otherwise(0.0);

        // 방문 여부 가중치 = 0.3
        NumberExpression<Double> visitedWeight = placeVisit.isVisited.when(true).then(0.3).otherwise(0.0);

        // 최종 가중치 계산식
        return baseWeight.add(likedWeight).add(visitedWeight);
    }

    @Override
    public Slice<PlacePreviewDto> recommendPlacesByActivityData(Long memberId, Double latitude, Double longitude,
                                                                List<Long> categoryIds,
                                                                Map<Long, Integer> hourMap,
                                                                int pageSize, int page) {
        // 1. 추천 장소 ID 리스트 추출 (필터링 및 정렬 로직 분리)
        List<Long> targetIds = findTargetIds(memberId, categoryIds, hourMap, latitude, longitude, pageSize, page - 1);

        if (targetIds.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), false);
        }

        // 2. 추출된 ID들에 대한 상세 정보 및 집계 조회
        List<PlacePreviewDto> results = fetchPlacePreviewsByIds(memberId, targetIds, categoryIds, latitude, longitude);

        boolean hasNext = results.size() > pageSize;
        if (hasNext) {
            results = results.subList(0, pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(page, pageSize), hasNext);
    }

    private List<Long> findTargetIds(Long memberId, List<Long> categoryIds, Map<Long, Integer> hourMap,
                                     Double userLat, Double userLon, int pageSize, int page) {

        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 바운딩 박스 설정 (위경도 약 11km 반경)
        double delta = 0.1;
        double minLat = userLat - delta;
        double maxLat = userLat + delta;
        double minLon = userLon - delta;
        double maxLon = userLon + delta;

        BooleanExpression[] hourConditions = buildHourConditions(categoryIds, hourMap);

        var query = queryFactory
                .select(place.id)
                .from(place)
                .where(
                        place.category.id.in(categoryIds),
                        place.latitude.between(minLat, maxLat),
                        place.longitude.between(minLon, maxLon),
                        notContainVisited(memberId)
                );

        if (hourConditions.length > 0) {
            query.where(ExpressionUtils.anyOf(hourConditions));
        }

        return query
                .orderBy(
                        buildCategoryOrder(categoryIds).asc(),
                        buildDistanceExpression(userLat, userLon).asc()
                )
                .offset((long) page * pageSize)
                .limit(pageSize + 1)
                .fetch();
    }

    private List<PlacePreviewDto> fetchPlacePreviewsByIds(Long memberId, List<Long> targetIds,
                                                          List<Long> categoryIds, Double userLat, Double userLon) {
        return queryFactory
                .select(Projections.constructor(PlacePreviewDto.class,
                        place.id,
                        place.placeName,
                        place.imageUrl,
                        category.name,
                        place.roadAddress,
                        place.activeTime,
                        placeReview.rating.avg().coalesce(0.0),
                        place.latitude,
                        place.longitude,
                        placeReview.count(),
                        placeLike.isLiked
                ))
                .from(place)
                .leftJoin(place.category, category)
                .leftJoin(placeReview).on(placeReview.place.id.eq(place.id))
                .leftJoin(placeLike).on(place.id.eq(placeLike.place.id).and(placeLike.member.id.eq(memberId)))
                .where(place.id.in(targetIds))
                .groupBy(place.id, place.placeName, place.imageUrl, category.name,
                        place.roadAddress, place.activeTime, place.latitude, place.longitude,
                        placeLike.id, placeLike.isLiked)
                .orderBy(buildCategoryOrder(categoryIds).asc(), buildDistanceExpression(userLat, userLon).asc())
                .fetch();
    }

    private NumberExpression<Double> buildDistanceExpression(Double lat, Double lon) {
        return Expressions.numberTemplate(Double.class,
                "(6371 * ACOS(COS(RADIANS({0})) * COS(RADIANS({1})) * COS(RADIANS({2}) - RADIANS({3})) + SIN(RADIANS({0})) * SIN(RADIANS({1}))))",
                lat, place.latitude, place.longitude, lon);
    }

    private NumberExpression<Integer> buildCategoryOrder(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Expressions.asNumber(0);
        }

        CaseBuilder caseBuilder = new CaseBuilder();

        CaseBuilder.Cases<Integer, ?> cases = caseBuilder.when(place.category.id.eq(categoryIds.get(0))).then(0);

        for (int i = 1; i < categoryIds.size(); i++) {
            cases = cases.when(place.category.id.eq(categoryIds.get(i))).then(i);
        }

        return Expressions.asNumber(cases.otherwise(categoryIds.size()));
    }

    private BooleanExpression[] buildHourConditions(List<Long> categoryIds, Map<Long, Integer> hourMap) {
        List<BooleanExpression> conditions = new ArrayList<>();
        for (Long id : categoryIds) {
            Integer avgHour = hourMap.get(id);
            if (avgHour != null) conditions.add(hourCondition(avgHour).and(place.category.id.eq(id)));
        }
        return conditions.toArray(new BooleanExpression[0]);
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
        QPlaceVisit subVisit = QPlaceVisit.placeVisit;

        return JPAExpressions
                .selectOne()
                .from(subVisit)
                .where(
                        subVisit.member.id.eq(memberId),
                        subVisit.place.id.eq(place.id)
                )
                .notExists();
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

        String normGroup = LocationUtils.extractUpperLocation(groupLocation);
        String normAlt = LocationUtils.extractUpperLocation(alternativeLocation);

        List<Place> places = queryFactory
                .selectFrom(place)
                .join(place.category, category).fetchJoin()
                .leftJoin(memberPlaceVote).on(memberPlaceVote.place.id.eq(place.id))
                .join(place.category, category)
                .where(
                        category.bigCategory.eq(bigCategory),
                        locationFilter(normGroup, normAlt)
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

    private BooleanExpression locationFilter(String normGroup, String normAlt) {
        BooleanExpression groupCond = normGroup.equals("전체 지역") ? null : place.sido.eq(normGroup);
        BooleanExpression altCond = normAlt.equals("전체 지역") ? null : place.sido.eq(normAlt);

        if (groupCond != null && altCond != null) return groupCond.or(altCond);
        return (groupCond != null) ? groupCond : altCond;
    }

    @Override
    public List<GptPlaceInfoDto> findPlacesWithCategoryAndReviewCount(List<Long> placeIds) {
        return queryFactory.select(Projections.constructor(
                        GptPlaceInfoDto.class,
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
    public Slice<PlaceSearchDto> searchPlace(int pageSize, String keyword, Integer lastRelevanceScore, Long lastPlaceId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), false);
        }

        NumberExpression<Integer> relevanceScore = getRelevanceScore(keyword);

        List<PlaceSearchDto> results = queryFactory.select(
                        Projections.constructor(PlaceSearchDto.class,
                                place.id,
                                place.imageUrl,
                                place.placeName,
                                place.category.bigCategory.stringValue(),
                                place.roadAddress,
                                place.activeTime,
                                placeReview.rating.avg().coalesce(0.0),
                                placeReview.count(),
                                relevanceScore
                        ))
                .from(place)
                .leftJoin(placeReview).on(placeReview.place.id.eq(place.id))
                .where(
                        keywordContains(keyword),
                        cursorCondition(relevanceScore, lastRelevanceScore, lastPlaceId)
                )
                .groupBy(place.id)
                .orderBy(relevanceScore.desc(), place.id.desc())
                .limit(pageSize + 1)
                .fetch();

        return checkLastPage(pageSize, results);
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

    private Slice<PlaceSearchDto> checkLastPage(int pageSize, List<PlaceSearchDto> results) {
        if (results.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), PageRequest.of(0, pageSize), false);
        }

        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
