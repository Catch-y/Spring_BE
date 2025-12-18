package umc.catchy.domain.mapping.memberCourse.dto.response;

import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.mapping.memberCourse.dto.query.MemberCourseDto;

import java.util.List;

public record MemberCourseResponse(
        Long courseId,
        CourseType courseType,
        String courseImage,
        String courseName,
        String courseDescription,
        List<String> categories
) {
    public static MemberCourseResponse from(MemberCourseDto dto, List<String> categories) {
        return new MemberCourseResponse(
                dto.getCourseId(),
                dto.getCourseType(),
                dto.getCourseImage(),
                dto.getCourseName(),
                dto.getCourseDescription(),
                categories
        );
    }
}
