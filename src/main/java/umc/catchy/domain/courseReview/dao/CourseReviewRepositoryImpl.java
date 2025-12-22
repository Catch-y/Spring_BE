package umc.catchy.domain.courseReview.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.courseReview.dto.response.CourseReviewImageResponse;
import umc.catchy.domain.courseReview.dto.response.CourseReviewResponse;
import umc.catchy.domain.reviewReport.dto.query.CourseReviewDto;
import umc.catchy.domain.reviewReport.dto.query.ReviewImageDto;

import java.util.List;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static umc.catchy.domain.course.domain.QCourse.course;
import static umc.catchy.domain.courseReview.domain.QCourseReview.courseReview;
import static umc.catchy.domain.courseReviewImage.domain.QCourseReviewImage.courseReviewImage;
import static umc.catchy.domain.mapping.placeCourse.domain.QPlaceCourse.placeCourse;
import static umc.catchy.domain.member.domain.QMember.member;

@RequiredArgsConstructor
public class CourseReviewRepositoryImpl implements CourseReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<CourseReviewResponse> getAllCourseReviewByCourseId(Long courseId, int pageSize, Long lastReviewId) {

        List<Long> reviewIds = queryFactory
                .select(courseReview.id)
                .from(courseReview)
                .where(
                        courseIdEq(courseId),
                        lastCourseReviewId(lastReviewId),
                        courseReview.isReported.eq(false)
                )
                .orderBy(courseReview.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        List<CourseReviewResponse> result = queryFactory.selectFrom(courseReview)
                .leftJoin(courseReview.member, member).on(courseReview.member.id.eq(member.id))
                .leftJoin(courseReviewImage).on(courseReviewImage.courseReview.id.eq(courseReview.id))
                .where(
                        courseReview.id.in(reviewIds)
                )
                .orderBy(courseReview.createdDate.desc())
                .transform(groupBy(courseReview.id).list(
                        Projections.constructor(CourseReviewResponse.class,
                                courseReview.id,
                                courseReview.comment,
                                list(Projections.constructor(CourseReviewImageResponse.class,
                                        courseReviewImage.id,
                                        courseReviewImage.imageUrl)),
                                courseReview.createdAt,
                                member.nickname
                        )
                ));

        return checkLastPage(pageSize, result);
    }

    @Override
    public Slice<CourseReviewDto> getAllCourseReviewByMemberId(Long memberId, int pageSize, Long lastReviewId) {
        List<Long> reviewIds = queryFactory
                .select(courseReview.id)
                .from(courseReview)
                .where(
                        memberIdEq(memberId),
                        lastCourseReviewId(lastReviewId)
                )
                .orderBy(courseReview.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        List<CourseReviewDto> result = queryFactory.selectFrom(courseReview)
                .leftJoin(courseReview.course, course)
                .leftJoin(courseReviewImage).on(courseReviewImage.courseReview.id.eq(courseReview.id))
                .leftJoin(placeCourse).on(placeCourse.course.id.eq(course.id))
                .where(courseReview.id.in(reviewIds))
                .orderBy(courseReview.createdDate.desc())
                .transform(groupBy(courseReview.id).list(
                        Projections.constructor(CourseReviewDto.class,
                                courseReview.id,
                                course.courseName,
                                courseReview.comment,
                                list(Projections.constructor(ReviewImageDto.class,
                                        courseReviewImage.id,
                                        courseReviewImage.imageUrl
                                )),
                                course.courseType,
                                list(placeCourse.place.category.bigCategory)
                        )
                ));

        return checkLastPageOfMyReviews(pageSize, result);
    }

    private BooleanExpression courseIdEq(Long courseId) {
        return courseId == null ? null : courseReview.course.id.eq(courseId);
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return memberId == null ? null : courseReview.member.id.eq(memberId);
    }

    private BooleanExpression lastCourseReviewId(Long lastReviewId) {
        if (lastReviewId == null) {
            return null;
        }
        return courseReview.id.lt(lastReviewId);
    }

    private Slice<CourseReviewResponse> checkLastPage(int pageSize, List<CourseReviewResponse> results) {
        boolean hasNext = false;
        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }
        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }

    private Slice<CourseReviewDto> checkLastPageOfMyReviews(int pageSize, List<CourseReviewDto> results) {
        boolean hasNext = false;
        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }
        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
