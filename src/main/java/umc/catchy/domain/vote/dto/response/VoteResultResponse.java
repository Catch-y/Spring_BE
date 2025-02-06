package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class VoteResultResponse {
    private String status;
    private int totalMembers;
    private List<VoteResult> results;

    public VoteResultResponse(String status, List<VoteResult> results) {
        this.status = status;
        this.results = results;
    }
}