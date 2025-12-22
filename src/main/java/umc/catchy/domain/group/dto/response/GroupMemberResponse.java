package umc.catchy.domain.group.dto.response;

import umc.catchy.domain.member.domain.Member;

public record GroupMemberResponse(
        Long memberId,
        String nickname,
        String profileImage
) {
    public static GroupMemberResponse fromEntity(Member member) {
        return new GroupMemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImage()
        );
    }
}
