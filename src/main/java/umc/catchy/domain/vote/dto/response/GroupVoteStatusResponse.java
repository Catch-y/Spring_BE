package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GroupVoteStatusResponse {
    private int totalMembers;
    private List<MemberVoteStatus> members;
}

