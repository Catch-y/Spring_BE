package umc.catchy.domain.courseReviewImage.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.courseReviewImage.domain.CourseReviewImage;

import java.util.List;

@Repository
public interface CourseReviewImageRepository extends JpaRepository<CourseReviewImage, Long> {
    void deleteAllByCourseReview(CourseReview courseReview);
    List<CourseReviewImage> findAllByCourseReview(CourseReview courseReview);
}
