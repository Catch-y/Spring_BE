package umc.catchy.domain.member.dto.response;

import umc.catchy.domain.member.domain.Member;

public record ProfileResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        String SocialType
) {
    public static ProfileResponse of(Member member) {
        return new ProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage(),
                member.getSocialType().getValue()
                );
    }
}
