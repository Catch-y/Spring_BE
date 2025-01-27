package umc.catchy.domain.member.dto.response;

import umc.catchy.domain.member.domain.Member;

public record ProfileResponse(
        Long id,
        String nickname,
        String profileImage
) {
    public static ProfileResponse of(Member member) {
        return new ProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImage()
                );
    }
}
