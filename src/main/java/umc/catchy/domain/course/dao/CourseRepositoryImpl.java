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
                course.participantsNumber.doubleValue().multiply(0.3)   //코스의 방문 수
                .add(course.rating.doubleValue().multiply(0.4)) //코스의 평점
                .add(Expressions.asNumber(  //코스의 리뷰 수 : query By courseReview
                                JPAExpressions
                                        .select(courseReview.count())
                                        .from(courseReview)
                                        .where(courseReview.course.id.eq(course.id))
                                ).multiply(0.2)
                        )
                        .add(Expressions.asNumber(  //코스의 북마크 수 : query By memberCourse
                                JPAExpressions
                                        .select(memberCourse.count())
                                        .from(memberCourse)
                                        .where(memberCourse.course.id.eq(course.id))
                                ).multiply(0.1)
                        );

        return queryFactory.selectFrom(course)
                .orderBy(popularityScore.desc())
                .limit(10)
                .fetch();
    }
}
