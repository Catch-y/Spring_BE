package umc.catchy.domain.mapping.memberGroup.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.domain.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberGroupRepository extends JpaRepository<MemberGroup, Long> {
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);
    Optional<MemberGroup> findByGroupIdAndMemberId(Long groupId, Long memberId);
    @Query("SELECT mg FROM MemberGroup mg WHERE mg.member.id = :memberId")
    Slice<MemberGroup> findAllByMemberId(@Param("memberId") Long memberId, Pageable pageable);
    int countByGroupId(Long groupId);
    @Query("SELECT mg.member FROM MemberGroup mg WHERE mg.group.id = :groupId")
    List<Member> findMembersByGroupId(@Param("groupId") Long groupId);
}