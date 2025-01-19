package umc.catchy.domain.course.dao;

import java.util.List;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;
import umc.catchy.domain.member.domain.Member;

public interface CourseRepositoryCustom {
    List<Course> findCourses(
            CourseType courseType,
            String upperLocation,
            String lowerLocation,
            Member member,
            Long lastId
    );
}
