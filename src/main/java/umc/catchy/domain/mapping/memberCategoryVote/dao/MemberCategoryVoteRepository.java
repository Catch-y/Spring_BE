package umc.catchy.domain.mapping.memberCategoryVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.catchy.domain.mapping.memberCategoryVote.domain.MemberCategoryVote;
import umc.catchy.domain.member.domain.Member;

import java.util.List;

public interface MemberCategoryVoteRepository extends JpaRepository<MemberCategoryVote, Long> {
    @Query("SELECT m FROM MemberCategoryVote mcv JOIN mcv.member m WHERE mcv.categoryVote.id = :categoryVoteId")
    List<Member> findMembersByCategoryVoteId(@Param("categoryVoteId") Long categoryVoteId);

    @Query("SELECT COUNT(mcv) > 0 FROM MemberCategoryVote mcv WHERE mcv.categoryVote.vote.id = :voteId AND mcv.member.id = :memberId")
    boolean existsByVoteIdAndMemberId(@Param("voteId") Long voteId, @Param("memberId") Long memberId);
    @Query("SELECT COUNT(DISTINCT mcv.member.id) FROM MemberCategoryVote mcv WHERE mcv.voteId = :voteId")
    int countDistinctMembersByVoteId(@Param("voteId") Long voteId);
    @Query("SELECT COUNT(mcv) " +
            "FROM MemberCategoryVote mcv " +
            "WHERE mcv.categoryVote.vote.id = :voteId " +
            "AND mcv.categoryVote.id = :categoryVoteId")
    int countByVoteIdAndCategoryVoteId(@Param("voteId") Long voteId, @Param("categoryVoteId") Long categoryVoteId);
    Integer deleteAllByMember(Member member);

    @Modifying
    @Query("DELETE FROM MemberCategoryVote m WHERE m.voteId = :voteId AND m.member.id = :memberId")
    void deleteByVoteIdAndMemberId(@Param("voteId") Long voteId, @Param("memberId") Long memberId);
}
