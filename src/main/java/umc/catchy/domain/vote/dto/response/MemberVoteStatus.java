package umc.catchy.domain.vote.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberVoteStatus {
    private Long memberId;
    private String nickname;
    private String profileImage;
    private boolean hasVoted;

    // AllArgsConstructor로 모든 필드를 초기화하는 생성자를 추가
}