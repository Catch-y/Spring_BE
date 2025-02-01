package umc.catchy.domain.courseReview.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.courseReview.dto.response.PostCourseReviewResponse;

import java.util.List;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static umc.catchy.domain.courseReview.domain.QCourseReview.*;
import static umc.catchy.domain.courseReviewImage.domain.QCourseReviewImage.*;
import static umc.catchy.domain.member.domain.QMember.*;

@RequiredArgsConstructor
public class CourseReviewRepositoryImpl implements CourseReviewRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<PostCourseReviewResponse.newCourseReviewResponseDTO> getAllCourseReviewByCourseId(Long courseId, int pageSize, Long lastReviewId) {

        List<Long> reviewIds = queryFactory
                .select(courseReview.id)
                .from(courseReview)
                .where(
                        courseIdEq(courseId),
                        lastCourseReviewId(lastReviewId)
                )
                .orderBy(courseReview.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        List<PostCourseReviewResponse.newCourseReviewResponseDTO> result = queryFactory.selectFrom(courseReview)
                .leftJoin(courseReview.member, member).on(courseReview.member.id.eq(member.id))
                .leftJoin(courseReviewImage).on(courseReviewImage.courseReview.id.eq(courseReview.id))
                .where(
                        courseReview.id.in(reviewIds)
                )
                .orderBy(courseReview.createdDate.desc())
                .transform(groupBy(courseReview.id).list(
                        Projections.fields(PostCourseReviewResponse.newCourseReviewResponseDTO.class,
                                courseReview.id.as("reviewId"),
                                courseReview.comment.as("comment"),
                                list(
                                        Projections.fields(PostCourseReviewResponse.courseReviewImageResponseDTO.class,
                                                courseReviewImage.id.as("reviewImageId"),
                                                courseReviewImage.imageUrl.as("imageUrl"))
                                ).as("reviewImages"),
                                courseReview.createdAt.as("createdAt"),
                                courseReview.member.nickname.as("creatorNickname"))
                ));

        return checkLastPage(pageSize,result);
    }

    private BooleanExpression courseIdEq(Long courseId) {
        return courseId == null ? null : courseReview.course.id.eq(courseId);
    }

    private BooleanExpression lastCourseReviewId(Long lastReviewId) {
        if (lastReviewId == null) {
            return null;
        }
        return courseReview.id.lt(lastReviewId);
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
