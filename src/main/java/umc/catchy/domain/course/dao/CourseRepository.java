package umc.catchy.domain.course.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.course.domain.CourseType;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {
    List<Course> findTop2ByMemberIdAndCourseTypeOrderByCreatedDateDesc(Long memberId, CourseType courseType);
}
