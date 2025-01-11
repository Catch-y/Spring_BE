package umc.catchy.domain.mapping.memberGroup.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;

@Repository
public interface MemberGroupRepository extends JpaRepository<MemberGroup, Long> {
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);
}