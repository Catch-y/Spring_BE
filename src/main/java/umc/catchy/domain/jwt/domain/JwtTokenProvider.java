package umc.catchy.domain.jwt.domain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import java.security.SignatureException;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.config.jwt.JwtProperties;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String createToken(String email, Long validity) {
        Claims claims = Jwts.claims().setSubject(email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret())
                .compact();

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN); // 유효하지 않은 토큰 에러 반환
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED); // 만료된 토큰 에러 반환
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw new GeneralException(ErrorStatus.UNSUPPORTED_TOKEN); // 지원하지 않는 형식 토큰 에러 반환
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN); // 토큰의 클레임이 비어 있는 경우 에러 반환
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
