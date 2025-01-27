package umc.catchy.domain.course.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findTop5ByMemberIdAndCourseTypeOrderByCreatedDateDesc(Long memberId, CourseType courseType);

    @Query(value = "SELECT * FROM course c WHERE c.course_type = :courseType ORDER BY c.created_date DESC LIMIT :limit", nativeQuery = true)
    List<Course> findTopNByCourseType(@Param("courseType") String courseType, @Param("limit") int limit);
}
