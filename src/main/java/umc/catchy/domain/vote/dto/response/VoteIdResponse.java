package umc.catchy.domain.vote.dto.response;

public record VoteIdResponse(Long voteId) {
    public static VoteIdResponse of(Long voteId) {
        return new VoteIdResponse(voteId);
    }
}
