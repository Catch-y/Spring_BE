package umc.catchy.domain.member.dto.response;

import java.time.LocalDateTime;
import umc.catchy.domain.member.domain.Member;

public record SignUpResponse(
        Long id,
        String providerId,
        String email,
        String nickname,
        String profileImage,
        LocalDateTime createdDate
) {
    public static SignUpResponse of(Member member) {
        return new SignUpResponse(
                member.getId(),
                member.getProviderId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImage(),
                member.getCreatedDate()
        );
    }

}
