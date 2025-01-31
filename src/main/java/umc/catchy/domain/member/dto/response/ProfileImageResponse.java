package umc.catchy.domain.member.dto.response;

import umc.catchy.domain.member.domain.Member;

public record ProfileImageResponse(
        Long id,
        String profileImage
) {
    public static ProfileImageResponse of(Member member) {
        return new ProfileImageResponse(
                member.getId(),
                member.getProfileImage()
                );
    }
}
