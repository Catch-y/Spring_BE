package umc.catchy.domain.mapping.memberPlaceVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberPlaceVote.domain.MemberPlaceVote;
import umc.catchy.domain.member.domain.Member;

import java.util.List;

@Repository
public interface MemberPlaceVoteRepository extends JpaRepository<MemberPlaceVote, Long> {

    boolean existsByMemberIdAndPlaceIdAndVoteId(Long memberId, Long placeId, Long voteId);
    List<MemberPlaceVote> findByGroupId(Long groupId);
    @Query("SELECT mpv.member FROM MemberPlaceVote mpv WHERE mpv.place.id = :placeId")
    List<Member> findMembersByPlaceId(@Param("placeId") Long placeId);
}
