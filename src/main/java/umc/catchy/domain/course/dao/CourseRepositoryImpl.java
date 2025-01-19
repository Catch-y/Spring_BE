package umc.catchy.domain.course.dao;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.domain.QCourse;
import umc.catchy.domain.mapping.placeCourse.domain.QPlaceCourse;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.QMember;
import umc.catchy.domain.place.domain.QPlace;

@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 페이지 수 설정
    Pageable pageable = PageRequest.of(0, 10);

    QCourse qCourse = QCourse.course;
    QPlace qPlace = QPlace.place;
    QPlaceCourse qPlaceCourse = QPlaceCourse.placeCourse;
    QMember qMember = QMember.member;

    @Override
    public List<Course> findCourses(CourseType courseType, String upperLocation, String lowerLocation, Member member, Long lastId) {

        JPAQuery<Course> query = queryFactory
                .select(qCourse)
                .from(qPlaceCourse)
                .innerJoin(qPlaceCourse.course, qCourse)
                .innerJoin(qPlaceCourse.place, qPlace)
                .innerJoin(qPlaceCourse.course.member, qMember)
                .where(
                        qPlaceCourse.course.id.eq(qCourse.id),
                        qPlaceCourse.place.id.eq(qPlace.id),
                        qPlaceCourse.course.member.eq(member),
                        qCourse.courseType.eq(courseType),
                        upperLocationFilter(qPlace, upperLocation),
                        lowerLocationFilter(qPlace, lowerLocation)
                )
                .orderBy(qCourse.id.desc())
                .limit(pageable.getPageSize() + 1);

        // lastId보다 작은 courseId들을 불러옴
        if (lastId != null) {
            query.where(qCourse.id.lt(lastId));
        }

        List<Course> courses = query
                .distinct()
                .orderBy(qCourse.id.desc())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return courses;
    }

    private BooleanExpression upperLocationFilter(QPlace qPlace, String upperLocation) {
        if ("all".equals(upperLocation)) {
            return null;
        }
        return qPlace.roadAddress.startsWith(upperLocation + " ");
    }

    private BooleanExpression lowerLocationFilter(QPlace qPlace, String lowerLocation) {
        if ("all".equals(lowerLocation)) {
            return null;
        }
        return qPlace.roadAddress.contains(" " + lowerLocation);
    }
}
