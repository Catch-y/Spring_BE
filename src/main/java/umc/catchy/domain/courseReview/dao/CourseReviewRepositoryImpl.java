package umc.catchy.domain.courseReview.dao;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;

import java.util.List;

import static umc.catchy.domain.course.domain.QCourse.*;
import static umc.catchy.domain.courseReview.domain.QCourseReview.*;
import static umc.catchy.domain.courseReviewImage.domain.QCourseReviewImage.*;
import static umc.catchy.domain.member.domain.QMember.*;

@RequiredArgsConstructor
public class CourseReviewRepositoryImpl implements CourseReviewRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> searchAllReviewByCourseId(Long courseId, int pageSize, Long lastReviewId) {

        List<PostCourseReviewResponse.newCourseReviewResponseDTO> results = queryFactory
                .select(Projections.fields(PostCourseReviewResponse.newCourseReviewResponseDTO.class,
                        courseReview.id.as("reviewId"),
                        courseReview.comment.as("comment"),
                        courseReview.createdDate.as("visitedDate"),
                        courseReview.member.nickname.as("creatorNickname")))
                .from(courseReview)
                .leftJoin(courseReview.member, member).on(courseReview.member.id.eq(member.id))
                .leftJoin(courseReviewImage).on(courseReviewImage.courseReview.id.eq(courseReview.id))
                .where(
                        courseReview.course.id.eq(courseId),
                        lastReviewId(lastReviewId)
                )
                .orderBy(courseReview.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        for (PostCourseReviewResponse.newCourseReviewResponseDTO result : results) {
            List<PostCourseReviewResponse.courseReviewImageResponseDTO> imageResponse = queryFactory
                    .select(Projections.fields(PostCourseReviewResponse.courseReviewImageResponseDTO.class,
                            courseReviewImage.id.as("reviewImageId"),
                            courseReviewImage.imageUrl.as("imageUrl")
                    ))
                    .from(courseReviewImage)
                    .leftJoin(courseReviewImage.courseReview,courseReview).on(courseReviewImage.courseReview.id.eq(courseReview.id))
                    .where(courseReviewImage.courseReview.id.eq(result.getReviewId()))
                    .fetch();
            result.setReviewImages(imageResponse);
        }

        return checkLastPage(pageSize,results);
    }

    private BooleanExpression lastReviewId(Long reviewId) {
        if (reviewId == null) {
            return null;
        }
        return courseReview.id.lt(reviewId);
    }

    private Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> checkLastPage(int pageSize, List<PostCourseReviewResponse.newCourseReviewResponseDTO> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0,pageSize), hasNext);
    }
}
