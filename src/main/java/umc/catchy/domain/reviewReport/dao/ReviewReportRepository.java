package umc.catchy.domain.reviewReport.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.reviewReport.domain.ReviewReport;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
}
