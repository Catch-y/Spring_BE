package umc.catchy.domain.mapping.memberActivetime.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberActivetime.domain.MemberActiveTime;

public interface MemberActiveTimeRepository extends JpaRepository<MemberActiveTime, Long> {

}
