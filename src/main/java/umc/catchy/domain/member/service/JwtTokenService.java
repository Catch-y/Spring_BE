package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.catchy.domain.jwt.service.BlackTokenRedisService;
import umc.catchy.domain.jwt.service.RedisTokenService;
import umc.catchy.domain.member.dto.response.TokenPair;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;
    private final BlackTokenRedisService blackTokenRedisService;

    public TokenPair issueTokens(String email, Long memberId) {
        String accessToken = jwtUtil.createAccessToken(email, memberId);
        String refreshToken = jwtUtil.createRefreshToken(email, memberId);

        redisTokenService.saveRefreshToken(refreshToken, memberId);

        return new TokenPair(accessToken, refreshToken);
    }

    public TokenPair reissueTokens(String accessToken, String refreshToken) {
        validateRefreshToken(refreshToken);

        Long memberId = jwtUtil.getMemberIdFromToken(refreshToken);
        validateRefreshTokenInRedis(refreshToken, memberId);

        redisTokenService.deleteRefreshTokenByMemberId(memberId);
        addAccessTokenToBlacklist(accessToken);

        String email = jwtUtil.getEmailFromToken(refreshToken);
        return issueTokens(email, memberId);
    }

    public boolean isAccessTokenValid(String accessToken) {
        if (accessToken == null) {
            return false;
        }

        if (!jwtUtil.isTokenValid(accessToken)) {
            return false;
        }

        return !blackTokenRedisService.isTokenBlacklisted(accessToken);
    }

    public boolean isRefreshTokenValid(String refreshToken, Long memberId) {
        return redisTokenService.isRefreshTokenValid(refreshToken, memberId);
    }

    public void invalidateTokens(String accessToken, String refreshToken) {
        addAccessTokenToBlacklist(accessToken);
        deleteRefreshToken(refreshToken);
    }

    public String extractEmailFromToken(String token) {
        return jwtUtil.getEmailFromToken(token);
    }

    public Long extractMemberIdFromToken(String token) {
        return jwtUtil.getMemberIdFromToken(token);
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        }
    }

    private void validateRefreshTokenInRedis(String refreshToken, Long memberId) {
        if (!redisTokenService.isRefreshTokenValid(refreshToken, memberId)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }

    private void addAccessTokenToBlacklist(String accessToken) {
        if (accessToken != null && jwtUtil.isTokenValid(accessToken)) {
            blackTokenRedisService.addBlacklistedToken(accessToken);
        }
    }

    private void deleteRefreshToken(String refreshToken) {
        if (refreshToken != null && jwtUtil.isTokenValid(refreshToken)) {
            Long memberId = jwtUtil.getMemberIdFromToken(refreshToken);
            redisTokenService.deleteRefreshTokenByMemberId(memberId);
        }
    }
}
