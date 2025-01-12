package umc.catchy.domain.mapping.memberStyle.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberStyle.domain.MemberStyle;

public interface MemberStyleRepository extends JpaRepository<MemberStyle, Long> {
}
