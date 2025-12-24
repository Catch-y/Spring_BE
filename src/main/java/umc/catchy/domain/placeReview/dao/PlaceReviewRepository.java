package umc.catchy.domain.placeReview.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.place.domain.Place;
import umc.catchy.domain.placeReview.domain.PlaceReview;

import java.util.List;

@Repository
public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long>, PlaceReviewRepositoryCustom {
    List<PlaceReview> findAllByPlace(Place place);

    Long countByPlaceId(Long placeId);

    Integer countAllByMemberId(Long memberId);

    @Query("SELECT pr.place.id, COUNT(pr) FROM PlaceReview pr WHERE pr.place.id IN :placeIds GROUP BY pr.place.id")
    List<Object[]> countByPlaceIdsGroupByPlace(@Param("placeIds") List<Long> placeIds);
}
