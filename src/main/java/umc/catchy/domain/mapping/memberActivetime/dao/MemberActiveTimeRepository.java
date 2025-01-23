package umc.catchy.domain.mapping.memberActivetime.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberActivetime.domain.MemberActiveTime;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface MemberActiveTimeRepository extends JpaRepository<MemberActiveTime, Long> {
    List<MemberActiveTime> findByMemberId(Long memberId);

    @Query("SELECT mat FROM MemberActiveTime mat JOIN FETCH mat.activeTime at " +
            "WHERE mat.member.id = :memberId AND at.dayOfWeek = :today")
    List<MemberActiveTime> findActiveTimeByMemberIdAndDayOfWeek(@Param("memberId") Long memberId, @Param("today") DayOfWeek today);
}