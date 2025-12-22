package umc.catchy.domain.vote.dto.response;

import java.util.List;

public record CategoryVoteListResponse(Long voteId, List<CategoryInfo> categories) {
    public record CategoryInfo(Long categoryId, String name) {}

    public static CategoryVoteListResponse of(Long voteId, List<CategoryInfo> categories) {
        return new CategoryVoteListResponse(voteId, categories);
    }
}
