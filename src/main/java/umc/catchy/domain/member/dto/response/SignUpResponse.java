package umc.catchy.domain.member.dto.response;

import java.time.LocalDateTime;

import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;

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
    public static SignUpResponse of(Member member, String refreshToken) {
        return new SignUpResponse(
                member.getId(),
                member.getProviderId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage(),
                member.getCreatedDate(),
                member.getAccessToken(),
                refreshToken,
                member.getFcmInfo()
        );
    }

}
