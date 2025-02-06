package umc.catchy.domain.mapping.placeCourse.dao;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.mapping.placeCourse.dto.response.PlaceInfoResponse;


import java.util.List;

import static umc.catchy.domain.mapping.placeLike.domain.QPlaceLike.placeLike;
import static umc.catchy.domain.mapping.placeVisit.domain.QPlaceVisit.placeVisit;
import static umc.catchy.domain.member.domain.QMember.member;
import static umc.catchy.domain.place.domain.QPlace.*;
import static umc.catchy.domain.placeReview.domain.QPlaceReview.placeReview;

@RequiredArgsConstructor
public class PlaceCourseRepositoryImpl implements PlaceCourseRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<PlaceInfoResponse> searchPlaceByLiked(Long memberId, int pageSize, Long lastPlaceId) {
        List<PlaceInfoResponse> results = queryFactory.select(Projections.fields(PlaceInfoResponse.class,
                        place.id.as("placeId"),
                        place.imageUrl.as("imageUrl"),
                        place.placeName.as("placeName"),
                        place.category.name.as("categoryName"),
                        place.roadAddress.as("roadAddress"),
                        place.activeTime.as("activeTime"),
                        placeReview.rating.avg().coalesce(0.0).as("rating"),
                        placeReview.count().as("reviewCount")
                        ))
                .from(place)
                .leftJoin(placeLike).on(place.id.eq(placeLike.place.id))
                .leftJoin(placeLike.member, member).on(placeLike.member.id.eq(member.id))
                .leftJoin(placeReview).on(placeReview.place.id.eq(place.id))
                .where(
                        placeLike.member.id.eq(memberId),
                        lastPlaceId(lastPlaceId),
                        likedCondition
                )
                .groupBy(place.id)
                .orderBy(placeLike.place.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        return checkLastPage(pageSize,results);
    }

    private BooleanExpression likedCondition = placeLike.isLiked.eq(true);

    private BooleanExpression lastPlaceId(Long placeId) {
        if (placeId == null) {
            return null;
        }
        return place.id.lt(placeId);
    }

    private Slice<PlaceInfoResponse> checkLastPage(int pageSize, List<PlaceInfoResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0,pageSize), hasNext);
    }
}
