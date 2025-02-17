package umc.catchy.domain.vote.dto.response.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import umc.catchy.domain.vote.dto.response.vote.MemberVoteStatus;

import java.util.List;

@Getter
@AllArgsConstructor
public class GroupVoteStatusResponse {
    private int totalMembers;
    private List<MemberVoteStatus> members;
}

