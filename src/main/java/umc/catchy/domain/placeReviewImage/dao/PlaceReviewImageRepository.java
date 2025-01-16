package umc.catchy.domain.placeReviewImage.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.placeReviewImage.domain.PlaceReviewImage;

@Repository
public interface PlaceReviewImageRepository extends JpaRepository<PlaceReviewImage, Long> {
}
