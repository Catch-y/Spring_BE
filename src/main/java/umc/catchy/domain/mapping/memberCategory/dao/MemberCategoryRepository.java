package umc.catchy.domain.mapping.memberCategory.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.mapping.memberCategory.domain.MemberCategory;
import umc.catchy.domain.member.domain.Member;

import java.util.List;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {
    List<MemberCategory> findByMemberId(Long memberId);
    Integer deleteAllByMember(Member member);
}
