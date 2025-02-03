package umc.catchy.domain.course.dao;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.QCourse;
import umc.catchy.domain.courseReview.domain.QCourseReview;
import umc.catchy.domain.mapping.memberCourse.domain.QMemberCourse;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Course> findPopularCourses(){
        QCourse course=QCourse.course;
        QCourseReview courseReview=QCourseReview.courseReview;
        QMemberCourse memberCourse=QMemberCourse.memberCourse;

        //인기코스 가중치 계산 : (방문 수 * 0.3) + (평점 * 0.4) + (리뷰 수 * 0.2) + (찜하기 * 0.1)
        NumberExpression<Double> popularityScore =
                Expressions.numberTemplate(Double.class, "({0} * 0.3)", course.participantsNumber.coalesce(0L)) //코스의 방문 수
                        .add(Expressions.numberTemplate(Double.class, "({0} * 0.4)", course.rating.coalesce(0.0)))  //코스의 평점
                        .add(Expressions.numberTemplate(Double.class, "({0} * 0.2)",    //코스의 리뷰 개수
                                JPAExpressions
                                        .select(courseReview.count().coalesce(0L))
                                        .from(courseReview)
                                        .where(courseReview.course.id.eq(course.id))))
                        .add(Expressions.numberTemplate(Double.class, "({0} * 0.1)",    //코스의 찜하기 개수
                                JPAExpressions
                                        .select(memberCourse.count().coalesce(0L))
                                        .from(memberCourse)
                                        .where(memberCourse.course.id.eq(course.id).and(memberCourse.bookmark.eq(true)))));

        return queryFactory.selectFrom(course)
                .orderBy(popularityScore.desc())
                .limit(10)
                .fetch();
    }
}
