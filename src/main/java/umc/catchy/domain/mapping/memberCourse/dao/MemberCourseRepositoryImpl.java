package umc.catchy.domain.mapping.memberCourse.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;


import java.util.List;

import static umc.catchy.domain.course.domain.QCourse.course;
import static umc.catchy.domain.mapping.memberCourse.domain.QMemberCourse.*;
import static umc.catchy.domain.mapping.placeCourse.domain.QPlaceCourse.*;
import static umc.catchy.domain.member.domain.QMember.*;
import static umc.catchy.domain.place.domain.QPlace.*;

@RequiredArgsConstructor
public class MemberCourseRepositoryImpl implements MemberCourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<MemberCourseResponse> findCourseByBookmarks(Long memberId, int pageSize, Long lastCourseId) {

        List<MemberCourseResponse> results = queryFactory.select(Projections.constructor(MemberCourseResponse.class,
                        course.id,
                        course.courseType,
                        course.courseImage,
                        course.courseName,
                        course.courseDescription,
                        JPAExpressions
                                .select(place.category.bigCategory)
                                .from(placeCourse)
                                .leftJoin(placeCourse.place, place)
                                .on(placeCourse.place.id.eq(place.id))
                                .where(placeCourse.course.id.eq(course.id))
                )).from(memberCourse)
                .leftJoin(memberCourse.course, course)
                .on(memberCourse.course.id.eq(course.id))
                .leftJoin(memberCourse.member, member)
                .on(memberCourse.member.id.eq(member.id))
                .where(
                        memberCourse.member.id.eq(memberId),
                        lastCourseId(lastCourseId),
                        markedCondition
                ).orderBy(course.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        return checkLastPage(pageSize,results);
    }

    private BooleanExpression markedCondition = memberCourse.bookmark.eq(true);

    private BooleanExpression lastCourseId(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return course.id.lt(courseId);
    }

    private Slice<MemberCourseResponse> checkLastPage(int pageSize, List<MemberCourseResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0,pageSize), hasNext);
    }

}
