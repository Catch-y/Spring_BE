package umc.catchy.domain.member.dto.response;

import java.time.LocalDateTime;
import umc.catchy.domain.member.domain.Member;

public record LoginResponse(
        Long id,
        Long providerId,
        String email,
        String nickname,
        LocalDateTime createdDate,
        String access_token,
        String refresh_token
) {
    public static LoginResponse of(Member member, String access_token, String refresh_token) {
        return new LoginResponse(
                member.getId(),
                member.getProviderId(),
                member.getEmail(),
                member.getNickname(),
                member.getCreatedDate(),
                access_token,
                refresh_token
        );
    }
}
