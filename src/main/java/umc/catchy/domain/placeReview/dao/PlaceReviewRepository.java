package umc.catchy.domain.placeReview.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.domain.PlaceReview;

import java.util.List;

@Repository
public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {
    List<PlaceReview> findAllByPlace(Place place);
}
