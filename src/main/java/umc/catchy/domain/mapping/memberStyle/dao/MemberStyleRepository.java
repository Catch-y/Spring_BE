package umc.catchy.domain.mapping.memberStyle.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberStyle.domain.MemberStyle;

@Repository
public interface MemberStyleRepository extends JpaRepository<MemberStyle, Long> {
}
