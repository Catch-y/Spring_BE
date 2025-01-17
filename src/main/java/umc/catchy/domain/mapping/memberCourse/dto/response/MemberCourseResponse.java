package umc.catchy.domain.mapping.memberCourse.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.CourseType;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberCourseResponse {
    Long courseId;
    CourseType courseType;
    String courseImage;
    String courseName;
    String courseDescription;
    List<BigCategory> categories;
}
