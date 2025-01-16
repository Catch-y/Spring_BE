package umc.catchy.domain.courseReviewImage.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.courseReviewImage.domain.CourseReviewImage;

@Repository
public interface CourseReviewImageRepository extends JpaRepository<CourseReviewImage, Long> {
}
