package umc.catchy.domain.mapping.memberCategoryVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.catchy.domain.mapping.memberCategoryVote.domain.MemberCategoryVote;

public interface MemberCategoryVoteRepository extends JpaRepository<MemberCategoryVote, Long> {
    @Query("SELECT CASE WHEN COUNT(mcv) > 0 THEN TRUE ELSE FALSE END " +
            "FROM MemberCategoryVote mcv " +
            "WHERE mcv.member.id = :memberId AND mcv.categoryVote.vote.id = :voteId")
    boolean existsByMemberIdAndVoteId(@Param("memberId") Long memberId, @Param("voteId") Long voteId);
}
