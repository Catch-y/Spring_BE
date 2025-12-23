package umc.catchy.domain.mapping.memberGroup.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberGroup.domain.MemberGroup;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberGroupRepository extends JpaRepository<MemberGroup, Long> {
    boolean existsByGroupIdAndMemberId(Long groupId, Long memberId);

    Optional<MemberGroup> findByGroupIdAndMemberId(Long groupId, Long memberId);

    int countByGroupId(Long groupId);

    @Query("SELECT mg.member FROM MemberGroup mg WHERE mg.group.id = :groupId")
    List<Member> findMembersByGroupId(@Param("groupId") Long groupId);

    List<MemberGroup> findAllByMemberId(Long memberId);

    @Query("SELECT mg FROM MemberGroup mg JOIN FETCH mg.group g WHERE mg.member.id = :memberId AND g.promiseTime >= :start AND g.promiseTime < :end")
    List<MemberGroup> findAllByMemberIdAndDateRange(@Param("memberId") Long memberId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}