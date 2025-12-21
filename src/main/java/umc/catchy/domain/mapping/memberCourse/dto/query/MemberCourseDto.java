package umc.catchy.domain.mapping.memberCourse.dto.query;

import lombok.Getter;
import umc.catchy.domain.course.domain.CourseType;

@Getter
public class MemberCourseDto {
    private final Long courseId;
    private final CourseType courseType;
    private final String courseImage;
    private final String courseName;
    private final String courseDescription;

    public MemberCourseDto(Long courseId, CourseType courseType, String courseImage,
                           String courseName, String courseDescription) {
        this.courseId = courseId;
        this.courseType = courseType;
        this.courseImage = courseImage;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
    }
}
