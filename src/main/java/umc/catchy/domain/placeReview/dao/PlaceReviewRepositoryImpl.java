package umc.catchy.domain.placeReview.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;

import java.util.List;
import java.util.Optional;

import static com.querydsl.core.group.GroupBy.*;
import static umc.catchy.domain.member.domain.QMember.*;
import static umc.catchy.domain.placeReview.domain.QPlaceReview.placeReview;
import static umc.catchy.domain.placeReviewImage.domain.QPlaceReviewImage.*;

@RequiredArgsConstructor
public class PlaceReviewRepositoryImpl implements PlaceReviewRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public List<PostPlaceReviewResponse.placeReviewRatingResponseDTO> findRatingList(Long placeId) {
        JPAQuery<PostPlaceReviewResponse.placeReviewRatingResponseDTO> query = queryFactory.select(Projections.constructor(PostPlaceReviewResponse.placeReviewRatingResponseDTO.class,
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
    public Slice<PostPlaceReviewResponse.newPlaceReviewResponseDTO> findPlaceReviewSliceByPlaceId(Long placeId, int pageSize, Long lastPlaceReviewId) {
        List<Long> reviewIds = queryFactory
                .select(placeReview.id)
                .from(placeReview)
                .where(
                        placeIdEq(placeId),
                        lastPlaceReviewId(lastPlaceReviewId),
                        placeReview.isReported.eq(false)
                )
                .orderBy(placeReview.visitedDate.desc())
                .limit(pageSize + 1)
                .fetch();

        List<PostPlaceReviewResponse.newPlaceReviewResponseDTO> result = queryFactory.selectFrom(placeReview)
                .leftJoin(placeReview.member, member).on(placeReview.member.id.eq(member.id))
                .leftJoin(placeReviewImage).on(placeReviewImage.placeReview.id.eq(placeReview.id))
                .where(
                        placeReview.id.in(reviewIds)
                )
                .orderBy(placeReview.visitedDate.desc())
                .transform(groupBy(placeReview.id).list(
                        Projections.fields(PostPlaceReviewResponse.newPlaceReviewResponseDTO.class,
                                placeReview.id.as("reviewId"),
                                placeReview.comment.as("comment"),
                                placeReview.rating.as("rating"),
                                list(
                                        Projections.fields(PostPlaceReviewResponse.placeReviewImageResponseDTO.class,
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

    private BooleanExpression placeIdEq(Long placeId) {
        return placeId == null ? null : placeReview.place.id.eq(placeId);
    }

    private BooleanExpression lastPlaceReviewId(Long placeReviewId) {
        if (placeReviewId == null) {
            return null;
        }
        return placeReview.id.lt(placeReviewId);
    }

    private Slice<PostPlaceReviewResponse.newPlaceReviewResponseDTO> checkLastPage(int pageSize, List<PostPlaceReviewResponse.newPlaceReviewResponseDTO> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0,pageSize), hasNext);
    }
}
