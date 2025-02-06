package umc.catchy.domain.course.dao;

import umc.catchy.domain.course.domain.Course;

import java.util.List;

public interface CourseRepositoryCustom {
    List<Course> findPopularCourses();
}
