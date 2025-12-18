package umc.catchy.domain.mapping.memberCourse.dto.response;

import umc.catchy.domain.course.domain.CourseType;

import java.util.List;

public record MemberCourseDto(
        Long courseId,
        CourseType courseType,
        String courseImage,
        String courseName,
        String courseDescription
) {
    public MemberCourseResponse toResponse(List<String> categories) {
        return new MemberCourseResponse(
                courseId,
                courseType,
                courseImage,
                courseName,
                courseDescription,
                categories
        );
    }
}

