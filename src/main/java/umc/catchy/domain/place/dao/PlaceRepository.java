package umc.catchy.domain.place.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.place.domain.Place;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
}
