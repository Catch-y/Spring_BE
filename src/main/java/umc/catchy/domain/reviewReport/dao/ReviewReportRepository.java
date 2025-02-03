package umc.catchy.domain.reviewReport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.courseReview.domain.CourseReview;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.reviewReport.domain.ReviewReport;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    void deleteAllByPlaceReview(PlaceReview placeReview);
    void deleteAllByCourseReview(CourseReview courseReview);
}
