package umc.catchy.domain.mapping.memberCourse.dao;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import umc.catchy.domain.place.domain.QPlace;

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
                course.courseDescription))
                .from(memberCourse)
                .leftJoin(memberCourse.course,course).on(memberCourse.course.id.eq(course.id))
                .leftJoin(memberCourse.member,member).on(memberCourse.member.id.eq(member.id))
                .where(
                        memberCourse.member.id.eq(memberId),
                        lastCourseId(lastCourseId),
                        markedCondition
                )
                .orderBy(course.createdDate.desc())
                .limit(pageSize+1)
                .fetch();

        for (MemberCourseResponse result : results) {
            List<BigCategory> bigCategoriesDuplicates = queryFactory.select(placeCourse.place.category.bigCategory)
                    .from(placeCourse)
                    .innerJoin(placeCourse.place,place).on(placeCourse.place.id.eq(place.id))
                    .innerJoin(placeCourse.course,course).on(placeCourse.course.id.eq(course.id))
                    .where(placeCourse.course.id.eq(result.getCourseId()))
                    .fetch();
            List<BigCategory> bigCategories = new ArrayList<>(new HashSet<>(bigCategoriesDuplicates));
            List<String> bigCategoryStrings = bigCategories.stream().map(BigCategory::getValue).toList();
            result.setCategories(bigCategoryStrings);
        }

        return checkLastPage(pageSize,results);
    }

    @Override
    public Slice<MemberCourseResponse> findCourseByFilters(CourseType courseType, String upperLocation,
                                                           String lowerLocation, Long memberId, Long lastCourseId) {
        List<MemberCourseResponse> results = queryFactory.select(Projections.constructor(MemberCourseResponse.class,
                        course.id,
                        course.courseType,
                        course.courseImage,
                        course.courseName,
                        course.courseDescription))
                .from(memberCourse)
                .leftJoin(memberCourse.course,course).on(memberCourse.course.id.eq(course.id))
                .leftJoin(memberCourse.member,member).on(memberCourse.member.id.eq(member.id))
                .where(
                        memberCourse.member.id.eq(memberId),
                        course.courseType.eq(courseType),
                        lastCourseId(lastCourseId),
                        upperLocationFilter(upperLocation),
                        lowerLocationFilter(lowerLocation)
                )
                .orderBy(course.createdDate.desc())
                .limit(11)
                .fetch();

        for (MemberCourseResponse result : results) {
            List<BigCategory> bigCategoriesDuplicates = queryFactory.select(placeCourse.place.category.bigCategory)
                    .from(placeCourse)
                    .innerJoin(placeCourse.place,place).on(placeCourse.place.id.eq(place.id))
                    .innerJoin(placeCourse.course,course).on(placeCourse.course.id.eq(course.id))
                    .where(placeCourse.course.id.eq(result.getCourseId()))
                    .fetch();
            List<BigCategory> bigCategories = new ArrayList<>(new HashSet<>(bigCategoriesDuplicates));
            List<String> bigCategoryStrings = bigCategories.stream().map(BigCategory::getValue).toList();
            result.setCategories(bigCategoryStrings);
        }

        return checkLastPage(10, results);
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

    private BooleanExpression upperLocationFilter(String upperLocation) {
        if ("all".equals(upperLocation)) {
            return null;
        }
        return place.roadAddress.startsWith(upperLocation + " ");
    }

    private BooleanExpression lowerLocationFilter(String lowerLocation) {
        if ("all".equals(lowerLocation)) {
            return null;
        }
        return place.roadAddress.contains(" " + lowerLocation);
    }

}
