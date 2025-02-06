package umc.catchy.domain.mapping.memberCourse.dao;

import org.springframework.data.domain.Slice;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;

public interface MemberCourseRepositoryCustom {

    Slice<MemberCourseResponse> findCourseByBookmarks(Long memberId, int pageSize, Long lastCourseId);
    Slice<MemberCourseResponse> findCourseByFilters(CourseType courseType, String upperLocation, String lowerLocation, Long memberId, Long lastCourseId);
}
