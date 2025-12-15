package umc.catchy.domain.course.dto.response;

import umc.catchy.domain.course.domain.Course;

public record PopularCourseInfoResponse(
        Long courseId,
        String courseImage,
        String courseName
) {
    public static PopularCourseInfoResponse from(Course course) {
        return new PopularCourseInfoResponse(
                course.getId(),
                course.getCourseImage(),
                course.getCourseName()
        );
    }
}
