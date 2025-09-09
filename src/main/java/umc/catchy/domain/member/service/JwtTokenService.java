package umc.catchy.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.catchy.domain.jwt.service.BlackTokenRedisService;
import umc.catchy.domain.jwt.service.RedisTokenService;
import umc.catchy.domain.member.dto.response.TokenPair;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JwtUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;
    private final BlackTokenRedisService blackTokenRedisService;

    /* 토큰 발급 */
    public TokenPair issueTokens(String email, Long memberId) {
        // 1. 토큰 생성
        String accessToken = jwtUtil.createAccessToken(email, memberId);
        String refreshToken = jwtUtil.createRefreshToken(email, memberId);

        // 2. RefreshToken만 Redis에 저장
        redisTokenService.saveRefreshToken(refreshToken, memberId);

        log.info("Tokens issued for memberId: {}", memberId);
        return new TokenPair(accessToken, refreshToken);
    }

    /* RefreshToken으로 새 토큰 발급 */
    public TokenPair reissueTokens(String accessToken, String refreshToken) {
        // 1. 리프레시 토큰 유효성 검사
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new GeneralException(ErrorStatus.TOKEN_EXPIRED);
        }

        // 2. memberId 추출
        Long memberId = jwtUtil.getMemberIdFromToken(refreshToken);

        // 3. RefreshToken 유효성 확인
        if (!redisTokenService.isRefreshTokenValid(refreshToken, memberId)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        // 4. 기존 RefreshToken 삭제
        redisTokenService.deleteRefreshTokenByMemberId(memberId);

        // 5. 기존 AccessToken 블랙리스트 추가
        if (accessToken != null && jwtUtil.isTokenValid(accessToken)) {
            blackTokenRedisService.addBlacklistedToken(accessToken);
        }

        // 6. 새로운 토큰 발급
        String email = jwtUtil.getEmailFromToken(refreshToken);
        log.info("Tokens reissued for memberId: {}", memberId);
        return issueTokens(email, memberId);
    }

    /* AccessToken 유효성 검증 */
    public boolean isAccessTokenValid(String accessToken) {
        if (accessToken == null) {
            return false;
        }

        // 1. JWT 자체 유효성 검증
        if (!jwtUtil.isTokenValid(accessToken)) {
            return false;
        }

        // 2. 블랙리스트 확인
        return !blackTokenRedisService.isTokenBlacklisted(accessToken);
    }

    /* RefreshToken 유효성 검증 */
    public boolean isRefreshTokenValid(String refreshToken, Long memberId) {
        return redisTokenService.isRefreshTokenValid(refreshToken, memberId);
    }

    /* 토큰 무효화 */
    public void invalidateTokens(String accessToken, String refreshToken) {
        // 1. AccessToken 블랙리스트 추가
        if (accessToken != null && jwtUtil.isTokenValid(accessToken)) {
            blackTokenRedisService.addBlacklistedToken(accessToken);
        }

        // 2. RefreshToken 삭제
        if (refreshToken != null && jwtUtil.isTokenValid(refreshToken)) {
            Long memberId = jwtUtil.getMemberIdFromToken(refreshToken);
            redisTokenService.deleteRefreshTokenByMemberId(memberId);
        }

        log.info("Tokens invalidated");
    }

    /* 토큰에서 이메일 추출 */
    public String extractEmailFromToken(String token) {
        return jwtUtil.getEmailFromToken(token);
    }

    /* 토큰에서 memberId 추출 */
    public Long extractMemberIdFromToken(String token) {
        return jwtUtil.getMemberIdFromToken(token);
    }
}
