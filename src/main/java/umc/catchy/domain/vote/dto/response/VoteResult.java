package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class VoteResult {
    private String category;
    private int voteCount;
    private List<VotedMemberResponse> votedMembers;
    private int rank;
}
