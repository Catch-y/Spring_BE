package umc.catchy.domain.member.dto.response;

import umc.catchy.domain.member.domain.Member;

public record NicknameResponse(
        Long id,
        String nickname
) {
    public static NicknameResponse of(Member member) {
        return new NicknameResponse(
                member.getId(),
                member.getNickname()
                );
    }
}
