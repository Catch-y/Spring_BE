package umc.catchy.domain.jwt.domain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
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

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void validateToken(String token) {
        token = extractToken(token);

        if (token == null || token.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        }

        try {
            extractClaims(token);
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        } catch (Exception e) {
            log.error("Unexpected JWT validation error", e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecret())
                .parseClaimsJws(token)
                .getBody();
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }

    public String getEmailFromToken(String token) {
        token = extractToken(token);
        try {
            return extractClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        } catch (Exception e) {
            log.error("Error extracting email from token", e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public Long getMemberIdFromToken(String token) {
        token = extractToken(token);
        try {
            return extractClaims(token).get("memberId", Long.class);
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        } catch (Exception e) {
            log.error("Error extracting memberId from token", e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public Date getExpirationTime(String token) {
        token = extractToken(token);
        try {
            return extractClaims(token).getExpiration();
        } catch (Exception e) {
            log.error("Error extracting expiration time", e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public String getTokenType(String token) {
        token = extractToken(token);
        try {
            return extractClaims(token).get("type", String.class);
        } catch (Exception e) {
            log.error("Error extracting token type", e);
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(getTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }
}
