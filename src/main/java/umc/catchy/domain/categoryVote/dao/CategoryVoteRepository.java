package umc.catchy.domain.categoryVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import umc.catchy.domain.categoryVote.domain.CategoryVote;

import java.util.List;

public interface CategoryVoteRepository extends JpaRepository<CategoryVote, Long> {
    @Query("SELECT cv FROM CategoryVote cv WHERE cv.vote.id = :voteId")
    List<CategoryVote> findByVoteId(@Param("voteId") Long voteId);

    @Query("SELECT g.id FROM CategoryVote cv JOIN cv.vote v JOIN v.group g WHERE v.id = :voteId")
    Long findGroupIdByVoteId(@Param("voteId") Long voteId);
}
