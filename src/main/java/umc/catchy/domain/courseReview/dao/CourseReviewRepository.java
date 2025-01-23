package umc.catchy.domain.courseReview.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.course.domain.Course;
import umc.catchy.domain.courseReview.domain.CourseReview;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long>, CourseReviewRepositoryCustom {
    Integer countAllByCourse(Course course);
}
