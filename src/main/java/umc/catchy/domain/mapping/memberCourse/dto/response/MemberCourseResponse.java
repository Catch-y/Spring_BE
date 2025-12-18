package umc.catchy.domain.mapping.memberCourse.dto.response;

import umc.catchy.domain.course.domain.CourseType;

import java.util.List;

public record MemberCourseResponse(
        Long courseId,
        CourseType courseType,
        String courseImage,
        String courseName,
        String courseDescription,
        List<String> categories
) {
}
