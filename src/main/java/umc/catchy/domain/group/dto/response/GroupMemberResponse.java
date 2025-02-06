package umc.catchy.domain.group.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.catchy.domain.member.domain.Member;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberResponse {
    private Long memberId;
    private String nickname;
    private String profileImage;

    public static GroupMemberResponse fromEntity(Member member) {
        return GroupMemberResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .build();
    }
}
