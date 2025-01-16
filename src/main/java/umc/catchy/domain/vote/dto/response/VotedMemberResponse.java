package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VotedMemberResponse {
    private Long memberId;
    private String nickname;
    private String profileImage;
}