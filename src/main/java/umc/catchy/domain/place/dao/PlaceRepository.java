package umc.catchy.domain.place.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.place.domain.Place;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long>, PlaceCustomRepository {
    @Query("SELECT p FROM Place p WHERE p.category.bigCategory = :bigCategory " +
            "AND (p.roadAddress LIKE %:groupLocation% OR p.roadAddress LIKE %:alternativeLocation%)")
    List<Place> findByBigCategoryAndLocation(
            @Param("bigCategory") BigCategory bigCategory,
            @Param("groupLocation") String groupLocation,
            @Param("alternativeLocation") String alternativeLocation
    );

    List<Place> findAllByPoiIdIn(List<Long> poiIds);

    @Query("SELECT p FROM Place p LEFT JOIN FETCH p.category WHERE p.id = :placeId")
    Optional<Place> findByIdWithCategory(@Param("placeId") Long placeId);
}