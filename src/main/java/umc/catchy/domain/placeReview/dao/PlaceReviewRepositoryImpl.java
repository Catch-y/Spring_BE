package umc.catchy.domain.placeReview.dao;

import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewImageResponse;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewRatingResponse;
import umc.catchy.domain.placeReview.dto.response.PlaceReviewResponse;
import umc.catchy.domain.reviewReport.dto.query.PlaceReviewDto;
import umc.catchy.domain.reviewReport.dto.query.ReviewImageDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static umc.catchy.domain.member.domain.QMember.member;
import static umc.catchy.domain.place.domain.QPlace.place;
import static umc.catchy.domain.placeReview.domain.QPlaceReview.placeReview;
import static umc.catchy.domain.placeReviewImage.domain.QPlaceReviewImage.placeReviewImage;

@RequiredArgsConstructor
public class PlaceReviewRepositoryImpl implements PlaceReviewRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public List<PlaceReviewRatingResponse> findRatingList(Long placeId) {
        JPAQuery<PlaceReviewRatingResponse> query = queryFactory.select(
                        Projections.constructor(PlaceReviewRatingResponse.class,
                                placeReview.rating,
                                placeReview.rating.count())
                )
                .from(placeReview)
                .where(placeIdEq(placeId))
                .groupBy(placeReview.rating)
                .orderBy(placeReview.rating.asc());
        return query.fetch();
    }

    @Override
    public Slice<PlaceReviewResponse> findPlaceReviewSliceByPlaceId(Long placeId, int pageSize, LocalDate lastPlaceReviewDate, Long lastPlaceReviewId) {
        List<Long> reviewIds = queryFactory
                .select(placeReview.id)
                .from(placeReview)
                .where(
                        placeIdEq(placeId),
                        lastPlaceReviewCondition(lastPlaceReviewDate, lastPlaceReviewId),
                        placeReview.isReported.eq(false)
                )
                .orderBy(placeReview.visitedDate.desc(), placeReview.id.desc())
                .limit(pageSize + 1)
                .fetch();

        List<PlaceReviewResponse> result = queryFactory.selectFrom(placeReview)
                .leftJoin(placeReview.member, member).on(placeReview.member.id.eq(member.id))
                .leftJoin(placeReviewImage).on(placeReviewImage.placeReview.id.eq(placeReview.id))
                .where(
                        placeReview.id.in(reviewIds)
                )
                .orderBy(placeReview.visitedDate.desc(), placeReview.id.desc())
                .transform(groupBy(placeReview.id).list(
                        Projections.fields(PlaceReviewResponse.class,
                                placeReview.id.as("reviewId"),
                                placeReview.comment.as("comment"),
                                placeReview.rating.as("rating"),
                                list(
                                        Projections.fields(PlaceReviewImageResponse.class,
                                                placeReviewImage.id.as("reviewImageId"),
                                                placeReviewImage.imageUrl.as("imageUrl"))
                                ).as("reviewImages"),
                                placeReview.visitedDate.as("visitedDate"),
                                placeReview.member.nickname.as("creatorNickname"))
                ));
        return checkLastPage(pageSize, result);
    }

    @Override
    public Optional<Double> findAverageRatingByPlaceId(Long placeId) {
        Optional<Double> averageRating = Optional.ofNullable(queryFactory.select(placeReview.rating.avg())
                .from(placeReview)
                .where(placeIdEq(placeId),
                        JPAExpressions
                                .select(placeReview.count())
                                .from(placeReview)
                                .where(placeReview.place.id.eq(placeId))
                                .goe(1L))
                .fetchOne());
        return averageRating.map(Double::valueOf);
    }

    @Override
    public Slice<PlaceReviewDto> getAllPlaceReviewByMemberId(Long memberId, int pageSize, LocalDate lastPlaceReviewDate, Long lastPlaceReviewId) {
        List<Long> reviewIds = queryFactory
                .select(placeReview.id)
                .from(placeReview)
                .where(memberIdEq(memberId), lastPlaceReviewCondition(lastPlaceReviewDate, lastPlaceReviewId))
                .orderBy(placeReview.visitedDate.desc(), placeReview.id.desc())
                .limit(pageSize + 1)
                .fetch();

        List<PlaceReviewDto> results = queryFactory.selectFrom(placeReview)
                .leftJoin(placeReview.place, place)
                .leftJoin(placeReviewImage).on(placeReviewImage.placeReview.id.eq(placeReview.id))
                .where(placeReview.id.in(reviewIds))
                .orderBy(placeReview.visitedDate.desc(), placeReview.id.desc())
                .transform(groupBy(placeReview.id).list(
                        Projections.constructor(PlaceReviewDto.class,
                                placeReview.id,
                                place.placeName,
                                placeReview.comment,
                                list(Projections.constructor(ReviewImageDto.class,
                                        placeReviewImage.id,
                                        placeReviewImage.imageUrl
                                )),
                                place.category.bigCategory,
                                placeReview.rating,
                                placeReview.visitedDate
                        )
                ));

        return checkLastPageOfMyReviews(pageSize, results);
    }

    @Override
    public Map<Long, Long> countReviewByPlaceIds(List<Long> placeIds) {
        return queryFactory
                .from(placeReview)
                .where(placeReview.place.id.in(placeIds))
                .groupBy(placeReview.place.id)
                .transform(GroupBy.groupBy(placeReview.place.id)
                        .as(placeReview.count()));
    }

    private BooleanExpression placeIdEq(Long placeId) {
        return placeId == null ? null : placeReview.place.id.eq(placeId);
    }

    private BooleanExpression lastPlaceReviewDate(LocalDate lastPlaceReviewDate) {
        if (lastPlaceReviewDate == null) {
            return null;
        }
        return placeReview.visitedDate.lt(lastPlaceReviewDate);
    }

    private BooleanExpression lastPlaceReviewCondition(LocalDate lastVisitedDate, Long lastReviewId) {
        if (lastVisitedDate == null || lastReviewId == null) {
            return null;  // 첫 페이지 요청 시에는 조건 없이 모든 데이터를 조회
        }

        // 방문 날짜가 마지막 방문 날짜보다 이전인 경우
        BooleanExpression beforeVisitedDate = placeReview.visitedDate.lt(lastVisitedDate);

        // 방문 날짜가 같고, 리뷰 ID가 마지막 리뷰 ID보다 작은 경우
        BooleanExpression sameDateBeforeId = placeReview.visitedDate.eq(lastVisitedDate)
                .and(placeReview.id.lt(lastReviewId));

        // 두 조건 중 하나라도 만족하면 해당 데이터를 가져옴
        return beforeVisitedDate.or(sameDateBeforeId);
    }

    private Slice<PlaceReviewResponse> checkLastPage(int pageSize, List<PlaceReviewResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0,pageSize), hasNext);
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return memberId == null ? null : placeReview.member.id.eq(memberId);
    }

    private Slice<PlaceReviewDto> checkLastPageOfMyReviews(int pageSize, List<PlaceReviewDto> results) {
        boolean hasNext = false;
        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }
        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
