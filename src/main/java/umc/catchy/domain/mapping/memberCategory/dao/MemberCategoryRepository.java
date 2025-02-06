package umc.catchy.domain.mapping.memberCategory.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;

import java.util.List;
import umc.catchy.domain.member.domain.Member;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {
    List<MemberCategory> findByMemberId(Long memberId);
    Integer deleteAllByMember(Member member);
}
