package umc.catchy.domain.mapping.memberStyle.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberStyle.domain.MemberStyle;

import java.util.List;
import umc.catchy.domain.member.domain.Member;

@Repository
public interface MemberStyleRepository extends JpaRepository<MemberStyle, Long> {
    List<MemberStyle> findByMemberId(Long memberId);
    Integer deleteAllByMember(Member member);
}
