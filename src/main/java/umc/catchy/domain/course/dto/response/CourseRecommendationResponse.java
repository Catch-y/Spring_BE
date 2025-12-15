package umc.catchy.domain.course.dto.response;

import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;

public record CourseRecommendationResponse(
        Long courseId,
        String courseName,
        String courseDescription,
        String courseImage,
        CourseType courseType
) {
    public static CourseRecommendationResponse from(Course course) {
        return new CourseRecommendationResponse(
                course.getId(),
                course.getCourseName(),
                course.getCourseDescription(),
                course.getCourseImage(),
                course.getCourseType()
        );
    }
}
