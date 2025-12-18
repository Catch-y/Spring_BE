package umc.catchy.domain.mapping.memberCourse.dao;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.course.util.LocationUtils;
import umc.catchy.domain.mapping.memberCourse.dto.query.MemberCourseDto;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;

import java.util.*;
import java.util.stream.Collectors;

import static umc.catchy.domain.category.domain.QCategory.category;
import static umc.catchy.domain.course.domain.QCourse.course;
import static umc.catchy.domain.mapping.memberCourse.domain.QMemberCourse.memberCourse;
import static umc.catchy.domain.mapping.placeCourse.domain.QPlaceCourse.placeCourse;
import static umc.catchy.domain.member.domain.QMember.member;
import static umc.catchy.domain.place.domain.QPlace.place;

@RequiredArgsConstructor
public class MemberCourseRepositoryImpl implements MemberCourseRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private static final String ALL_LOCATION = "all";
    private static final String SPACE = " ";
    private static final int FILTER_PAGE_SIZE = 10;
    private static final int FILTER_FETCH_SIZE = FILTER_PAGE_SIZE + 1;

    @Override
    public Slice<MemberCourseResponse> findCourseByBookmarks(Long memberId, int pageSize, Long lastCourseId) {

        List<MemberCourseDto> dtos = queryFactory.select(Projections.constructor(MemberCourseDto.class,
                        course.id,
                        course.courseType,
                        course.courseImage,
                        course.courseName,
                        course.courseDescription))
                .from(memberCourse)
                .leftJoin(memberCourse.course, course).on(memberCourse.course.id.eq(course.id))
                .leftJoin(memberCourse.member, member).on(memberCourse.member.id.eq(member.id))
                .where(
                        memberCourse.member.id.eq(memberId),
                        lastCourseId(lastCourseId),
                        markedCondition
                )
                .orderBy(course.createdDate.desc())
                .limit(pageSize + 1)
                .fetch();

        List<MemberCourseResponse> results = fetchCategoriesAndBuildResponses(dtos);

        return checkLastPage(pageSize, results);
    }

    @Override
    public Slice<MemberCourseResponse> findCourseByFilters(CourseType courseType, String upperLocation,
                                                           String lowerLocation, Long memberId, Long lastCourseId) {
        List<MemberCourseDto> dtos = queryFactory
                .select(Projections.constructor(MemberCourseDto.class,
                        course.id,
                        course.courseType,
                        course.courseImage,
                        course.courseName,
                        course.courseDescription))
                .from(memberCourse)
                .leftJoin(memberCourse.course, course).on(memberCourse.course.id.eq(course.id))
                .leftJoin(memberCourse.member, member).on(memberCourse.member.id.eq(member.id))
                .leftJoin(placeCourse).on(course.id.eq(placeCourse.course.id))
                .leftJoin(place).on(placeCourse.place.id.eq(place.id))
                .where(
                        memberCourse.member.id.eq(memberId),
                        course.courseType.eq(courseType),
                        lastCourseId(lastCourseId),
                        upperLocationFilter(upperLocation),
                        lowerLocationFilter(lowerLocation)
                )
                .groupBy(memberCourse.id)
                .orderBy(course.createdDate.desc())
                .limit(FILTER_FETCH_SIZE)
                .fetch();

        List<MemberCourseResponse> results = fetchCategoriesAndBuildResponses(dtos);

        return checkLastPage(FILTER_PAGE_SIZE, results);
    }

    private List<MemberCourseResponse> fetchCategoriesAndBuildResponses(List<MemberCourseDto> dtos) {
        if (dtos.isEmpty()) {
            return List.of();
        }

        // 1. 모든 courseId 추출
        List<Long> courseIds = dtos.stream()
                .map(MemberCourseDto::getCourseId)
                .toList();

        // 2. IN 쿼리로 한 번에 조회
        List<Tuple> categoryTuples = queryFactory
                .select(
                        placeCourse.course.id,
                        placeCourse.place.category.bigCategory
                )
                .from(placeCourse)
                .innerJoin(placeCourse.place, place)
                .innerJoin(place.category, category)
                .where(placeCourse.course.id.in(courseIds))
                .fetch();

        // 3. Map으로 그룹핑
        Map<Long, List<BigCategory>> categoryMap = categoryTuples.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(placeCourse.course.id),
                        Collectors.mapping(
                                tuple -> tuple.get(placeCourse.place.category.bigCategory),
                                Collectors.toList()
                        )
                ));

        // 4. 결과 조합
        List<MemberCourseResponse> results = new ArrayList<>();
        for (MemberCourseDto dto : dtos) {
            List<BigCategory> categories = categoryMap.getOrDefault(dto.getCourseId(), List.of());
            List<BigCategory> uniqueCategories = new ArrayList<>(new HashSet<>(categories));
            List<String> categoryStrings = uniqueCategories.stream()
                    .map(BigCategory::getValue)
                    .toList();

            results.add(MemberCourseResponse.from(dto, categoryStrings));
        }

        return results;
    }

    private final BooleanExpression markedCondition = memberCourse.bookmark.eq(true);

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

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }

    private BooleanExpression upperLocationFilter(String upperLocation) {
        if (ALL_LOCATION.equals(upperLocation)) {
            return null;
        }
        return place.roadAddress.startsWith(LocationUtils.normalizeLocation(upperLocation) + SPACE);
    }

    private BooleanExpression lowerLocationFilter(String lowerLocation) {
        if (ALL_LOCATION.equals(lowerLocation)) {
            return null;
        }
        return place.roadAddress.contains(SPACE + lowerLocation);
    }
}
