package umc.catchy.domain.location.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.location.domain.Location;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByUpperLocationAndLowerLocation(String upperLocationName, String lowerLocationName);
}
