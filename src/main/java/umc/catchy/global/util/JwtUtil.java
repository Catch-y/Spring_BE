package umc.catchy.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.catchy.domain.jwt.domain.TokenProvider;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.infra.config.jwt.JWTProperties;

@Component
@RequiredArgsConstructor
public class JWTUtil {

    private final TokenProvider tokenProvider;
    private final JWTProperties jwtProperties;
    private final MemberRepository memberRepository;

    public String createAccessToken(String email) {
        return tokenProvider.createToken(email, jwtProperties.getAccessTokenValidity());
    }

    public String createRefreshToken(String email) {
        return tokenProvider.createToken(email, jwtProperties.getRefreshTokenValidity());
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    public String getEmailFromToken(String token) {
        return tokenProvider.getEmailFromToken(token);
    }

    public Long getMemberIdFromToken(String token) {
        String email = getEmailFromToken(token);
        Member member = memberRepository.findByEmail(email).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 계정입니다.")
        );
        return member.getId();
    }
}
