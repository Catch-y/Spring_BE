package umc.catchy.domain.mapping.memberLocation.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;

@Repository
public interface MemberLocationRepository extends JpaRepository<MemberLocation, Long> {
    Long save(MemberLocation memberLocation);
}
