package umc.catchy.domain.vote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.vote.domain.Vote;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByIdAndGroupId(Long voteId, Long groupId);
}