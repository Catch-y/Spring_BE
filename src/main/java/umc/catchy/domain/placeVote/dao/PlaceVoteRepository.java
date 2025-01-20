package umc.catchy.domain.placeVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.placeVote.domain.PlaceVote;

public interface PlaceVoteRepository extends JpaRepository<PlaceVote, Long> {
}