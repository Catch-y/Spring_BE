package umc.catchy.domain.mapping.memberLocation.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberLocation.domain.MemberLocation;

import java.util.List;
import umc.catchy.domain.member.domain.Member;

@Repository
public interface MemberLocationRepository extends JpaRepository<MemberLocation, Long> {
    List<MemberLocation> findAllByMemberId(Long memberId);
    Integer deleteAllByMember(Member member);
}
