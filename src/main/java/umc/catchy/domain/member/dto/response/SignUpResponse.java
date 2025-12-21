package umc.catchy.domain.member.dto.response;

import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;

import java.time.LocalDateTime;

public record SignUpResponse(
        Long id,
        String providerId,
        String email,
        String nickname,
        String profileImage,
        LocalDateTime createdDate,
        String accessToken,
        String refreshToken,
        FcmInfo fcmInfo
) {
    public static SignUpResponse of(Member member, TokenPair tokens) {
        return new SignUpResponse(
                member.getId(),
                member.getProviderId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage(),
                member.getCreatedDate(),
                tokens.accessToken(),
                tokens.refreshToken(),
                member.getFcmInfo()
        );
    }

}
