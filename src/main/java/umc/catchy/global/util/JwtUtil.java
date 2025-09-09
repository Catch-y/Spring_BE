package umc.catchy.global.util;

import io.jsonwebtoken.ExpiredJwtException;

import java.util.Date;

import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import umc.catchy.domain.jwt.domain.JwtTokenProvider;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;

    public String createAccessToken(String email, Long memberId) {
        return jwtTokenProvider.createAccessToken(email, memberId);
    }

    public String createRefreshToken(String email, Long memberId) {
        return jwtTokenProvider.createRefreshToken(email, memberId);
    }

    public void validateToken(String token) {
        jwtTokenProvider.validateToken(token);
    }

    public String getEmailFromToken(String token) {
        return jwtTokenProvider.getEmailFromToken(token);
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (GeneralException e) {
            return false;
        }
    }

    public Long getMemberIdFromToken(String token) {
        try {
            return jwtTokenProvider.getMemberIdFromToken(token);
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        }
    }

    public String getTokenType(String token) {
        try {
            return jwtTokenProvider.getTokenType(token);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return jwtTokenProvider.isRefreshToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return jwtTokenProvider.isAccessToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpirationTime(String token) {
        try {
            return jwtTokenProvider.getExpirationTime(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.NOT_FOUND_TOKEN);
        }
    }
}
