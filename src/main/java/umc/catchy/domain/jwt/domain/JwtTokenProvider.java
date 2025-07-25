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

    /* 액세스 토큰 생성 */
    public String createAccessToken(String email, Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenValidity());

        return Jwts.builder()
                .setSubject(email)
                .claim("memberId", memberId)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret())
                .compact();

    }

    /* 리프레시 토큰 생성 */
    public String createRefreshToken(String email, Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenValidity());

        return Jwts.builder()
                .setSubject(email)
                .claim("memberId", memberId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret())
                .compact();

    }

    /* 토큰 검증 */
    public void validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("Token is null or empty");
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        }

        log.debug("token: {}", token);
        log.debug("secret: {}", jwtProperties.getSecret());

        try {
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecret())
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired while extracting claims: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            log.debug("Malformed token while extracting claims: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported token while extracting claims: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid token while extracting claims: {}", e.getMessage());
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        } catch (Exception e) {
            log.error("Unexpected error while extracting claims: {}", e.getMessage(), e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    /* 토큰에서 이메일 추출 */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /* 토큰에서 memberId 추출 */
    public Long getMemberIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
        return claims.get("memberId", Long.class);
    }

    public Date getExpirationTime(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    /* 토큰 타입 확인 */
    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
        return claims.get("type", String.class);
    }

    /* AccessToken인지 확인 */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    /* RefreshToken인지 확인 */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }
}
