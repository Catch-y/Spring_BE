package umc.catchy.domain.mapping.placeCourse.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.mapping.placeCourse.domain.PlaceCourse;

import java.util.List;

@Repository
public interface PlaceCourseRepository extends JpaRepository<PlaceCourse, Long> {
    List<PlaceCourse> findAllByCourse(@Param("course") Course course);
}
