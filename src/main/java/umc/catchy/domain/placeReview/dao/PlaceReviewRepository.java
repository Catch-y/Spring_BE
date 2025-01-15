package umc.catchy.domain.placeReview.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.placeReview.domain.PlaceReview;

@Repository
public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {
}
