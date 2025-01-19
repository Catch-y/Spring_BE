package umc.catchy.domain.mapping.memberPlaceVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import umc.catchy.domain.mapping.memberPlaceVote.domain.MemberPlaceVote;

import java.util.List;

@Repository
public interface MemberPlaceVoteRepository extends JpaRepository<MemberPlaceVote, Long> {

    boolean existsByMemberIdAndPlaceIdAndVoteId(Long memberId, Long placeId, Long voteId);
    List<MemberPlaceVote> findByGroupId(Long groupId);
}
