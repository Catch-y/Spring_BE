package umc.catchy.domain.vote.dto.response.vote;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResponse {

    private Long voteId;

    public static VoteResponse of(Long voteId) {
        return VoteResponse.builder()
                .voteId(voteId)
                .build();
    }
}