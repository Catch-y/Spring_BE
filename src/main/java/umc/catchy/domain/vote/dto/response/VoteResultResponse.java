package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VoteResultResponse {
    private String status;
    private int totalMembers;
    private List<VoteResult> results;
}