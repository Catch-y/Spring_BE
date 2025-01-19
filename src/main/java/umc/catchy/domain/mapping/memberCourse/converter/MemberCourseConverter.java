package umc.catchy.domain.mapping.memberCourse.converter;

import java.util.List;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.memberCourse.dto.response.MemberCourseResponse;

public class MemberCourseConverter {

    public static MemberCourseResponse toMemberCourseResponse(
            Course course,
            List<BigCategory> categories
    ) {
        return MemberCourseResponse.builder()
                .courseId(course.getId())
                .courseType(course.getCourseType())
                .courseImage(course.getCourseImage())
                .courseName(course.getCourseName())
                .courseDescription(course.getCourseDescription())
                .categories(categories)
                .createdDate(course.getCreatedDate())
                .build();
    }
}
