package umc.catchy.domain.categoryVote.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.catchy.domain.categoryVote.domain.CategoryVote;

public interface CategoryVoteRepository extends JpaRepository<CategoryVote, Long> {
}
