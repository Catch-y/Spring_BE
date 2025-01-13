package umc.catchy.domain.mapping.memberActivetime.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberActivetime.domain.MemberActiveTime;

@Repository
public interface MemberActiveTimeRepository extends JpaRepository<MemberActiveTime, Long> {
}
