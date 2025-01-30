package umc.catchy.domain.course.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.catchy.domain.course.domain.Course;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRecommendationResponse {
    private Long courseId;
    private String courseName;
    private String courseDescription;
    private String courseImage;
    private String courseType;

    public static CourseRecommendationResponse fromEntity(Course course, String courseType) {
        return CourseRecommendationResponse.builder()
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .courseImage(course.getCourseImage())
                .courseType(course.getCourseType().name()) // Course 엔티티의 courseType을 매핑
                .build();
    }
}
