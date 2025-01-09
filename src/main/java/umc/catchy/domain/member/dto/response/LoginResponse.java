package umc.catchy.domain.member.dto.response;

import java.time.LocalDateTime;
import umc.catchy.domain.member.domain.Member;

public record LoginResponse(
        Long id,
        String providerId,
        String email,
        String nickname,
        LocalDateTime createdDate,
        String accessToken,
        String refreshToken
) {
    public static LoginResponse of(Member member, String accessToken, String refreshToken) {
        return new LoginResponse(
                member.getId(),
                member.getProviderId(),
                member.getEmail(),
                member.getNickname(),
                member.getCreatedDate(),
                accessToken,
                refreshToken
        );
    }
}
