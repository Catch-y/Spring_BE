package umc.catchy.domain.placeReviewImage.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.placeReview.domain.PlaceReview;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;

import java.util.List;

@Repository
public interface PlaceReviewImageRepository extends JpaRepository<PlaceReviewImage, Long> {
    void deleteAllByPlaceReview(PlaceReview placeReview);
    List<PlaceReviewImage> findAllByPlaceReview(PlaceReview placeReview);
}
