package umc.catchy.domain.place.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.place.domain.Place;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    @Query("SELECT p FROM Place p WHERE p.category.bigCategory = :bigCategory AND p.roadAddress LIKE %:groupLocation%")
    List<Place> findByBigCategoryAndLocation(@Param("bigCategory") BigCategory bigCategory, @Param("groupLocation") String groupLocation);
}
