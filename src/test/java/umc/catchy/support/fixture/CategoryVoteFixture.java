package umc.catchy.support.fixture;

import umc.catchy.domain.category.domain.BigCategory;
import umc.catchy.domain.categoryVote.domain.CategoryVote;
import umc.catchy.domain.vote.domain.Vote;

public class CategoryVoteFixture {
    public static CategoryVote createCategoryVote(Long id, Vote vote, BigCategory category) {
        return CategoryVote.builder()
                .id(id)
                .vote(vote)
                .bigCategory(category)
                .build();
    }
}
