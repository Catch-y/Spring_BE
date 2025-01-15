package umc.catchy.domain.vote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.vote.domain.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}