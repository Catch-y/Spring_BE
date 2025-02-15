package umc.catchy.domain.vote.dto.response.vote;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberVoteStatus {
    private Long memberId;
    private String nickname;
    private String profileImage;
    private boolean hasVoted;
}