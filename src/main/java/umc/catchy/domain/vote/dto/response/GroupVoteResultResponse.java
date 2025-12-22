package umc.catchy.domain.vote.dto.response;

import java.util.List;

public record GroupVoteResultResponse(String groupLocation, List<CategoryResult> categories) {
    public record CategoryResult(String category, int count) {
        public static CategoryResult of(String category, int count) {
            return new CategoryResult(category, count);
        }
    }

    public static GroupVoteResultResponse of(String groupLocation, List<CategoryResult> categories) {
        return new GroupVoteResultResponse(groupLocation, categories);
    }
}
