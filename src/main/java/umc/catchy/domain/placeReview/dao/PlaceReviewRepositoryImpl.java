package umc.catchy.domain.placeReview.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReview.dto.response.PostPlaceReviewResponse;
import umc.catchy.domain.placeReviewImage.domain.QPlaceReviewImage;

import java.util.List;

import static umc.catchy.domain.placeReview.domain.QPlaceReview.placeReview;
import static umc.catchy.domain.placeReviewImage.domain.QPlaceReviewImage.*;

@RequiredArgsConstructor
public class PlaceReviewRepositoryImpl implements PlaceReviewRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public List<PlaceReview> findAllReviewsByPlaceId(Long placeId) {
        return queryFactory.selectFrom(placeReview)
                .join(placeReview.images, placeReviewImage)
                .fetchJoin()
                .where(placeIdEq(placeId))
                .orderBy(placeReview.visitDate.desc())
                .fetch();
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
