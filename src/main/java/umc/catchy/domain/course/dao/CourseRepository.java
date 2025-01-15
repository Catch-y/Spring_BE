package umc.catchy.domain.course.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
}
