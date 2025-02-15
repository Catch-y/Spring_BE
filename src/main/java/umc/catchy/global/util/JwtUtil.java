package umc.catchy.global.util;

import io.jsonwebtoken.Jwts;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.catchy.domain.jwt.domain.JwtTokenProvider;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.config.jwt.JwtProperties;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final MemberRepository memberRepository;

    public String createAccessToken(String email) {
        return jwtTokenProvider.createToken(email, jwtProperties.getAccessTokenValidity());
    }

    public String createRefreshToken(String email) {
        return jwtTokenProvider.createToken(email, jwtProperties.getRefreshTokenValidity());
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }

    public Long getMemberIdFromToken(String token) {
        Member member = memberRepository.findByAccessToken(token).orElseThrow(() ->
                new GeneralException(ErrorStatus.MEMBER_NOT_FOUND)
        );
        return member.getId();
    }

    public Date getExpirationTime(String accessToken) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(accessToken)
                .getBody()
                .getExpiration();
    }
}
