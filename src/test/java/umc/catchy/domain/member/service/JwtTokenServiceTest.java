package umc.catchy.domain.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.jwt.service.BlackTokenRedisService;
import umc.catchy.domain.jwt.service.RedisTokenService;
import umc.catchy.domain.member.dto.response.TokenPair;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @InjectMocks
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private BlackTokenRedisService blackTokenRedisService;

    @Test
    @DisplayName("토큰 발급 성공")
    void issueTokens_success() {
        // given
        String email = "test@test.com";
        Long memberId = 1L;
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(jwtUtil.createAccessToken(email, memberId)).thenReturn(accessToken);
        when(jwtUtil.createRefreshToken(email, memberId)).thenReturn(refreshToken);

        // when
        TokenPair tokens = jwtTokenService.issueTokens(email, memberId);

        // then
        assertAll(
                () -> assertThat(tokens.accessToken()).isEqualTo(accessToken),
                () -> assertThat(tokens.refreshToken()).isEqualTo(refreshToken)
        );

        verify(redisTokenService, times(1)).saveRefreshToken(refreshToken, memberId);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueTokens_success() {
        // given
        String oldAccessToken = "old-access-token";
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        String email = "test@test.com";
        Long memberId = 1L;

        // 1. Access/Refresh Token 유효성 검증 Mock
        when(jwtUtil.isTokenValid(oldAccessToken)).thenReturn(true);
        when(jwtUtil.isTokenValid(oldRefreshToken)).thenReturn(true);
        when(jwtUtil.getMemberIdFromToken(oldRefreshToken)).thenReturn(memberId);
        when(redisTokenService.isRefreshTokenValid(oldRefreshToken, memberId)).thenReturn(true);

        // 2. 새로운 토큰 발급 Mock
        when(jwtUtil.getEmailFromToken(oldRefreshToken)).thenReturn(email);
        when(jwtUtil.createAccessToken(email, memberId)).thenReturn(newAccessToken);
        when(jwtUtil.createRefreshToken(email, memberId)).thenReturn(newRefreshToken);

        // when
        TokenPair tokens = jwtTokenService.reissueTokens(oldAccessToken, oldRefreshToken);

        // then
        assertAll(
                () -> assertThat(tokens.accessToken()).isEqualTo(newAccessToken),
                () -> assertThat(tokens.refreshToken()).isEqualTo(newRefreshToken)
        );

        // 검증: 기존 RefreshToken 삭제 및 AccessToken 블랙리스트 추가 확인
        verify(redisTokenService, times(1)).deleteRefreshTokenByMemberId(memberId);
        verify(blackTokenRedisService, times(1)).addBlacklistedToken(oldAccessToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 Refresh Token")
    void reissueTokens_fail_expired() {
        // given
        String oldAccessToken = "old-access-token";
        String oldRefreshToken = "expired-refresh-token";

        when(jwtUtil.isTokenValid(oldRefreshToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> jwtTokenService.reissueTokens(oldAccessToken, oldRefreshToken))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("Access Token 유효성 검사 성공")
    void isAccessTokenValid_success() {
        // given
        String accessToken = "valid-access-token";
        when(jwtUtil.isTokenValid(accessToken)).thenReturn(true);
        when(blackTokenRedisService.isTokenBlacklisted(accessToken)).thenReturn(false);

        // when
        boolean result = jwtTokenService.isAccessTokenValid(accessToken);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Access Token 유효성 검사 실패 - 블랙리스트 등재됨")
    void isAccessTokenValid_fail_blacklisted() {
        // given
        String accessToken = "blacklisted-access-token";
        when(jwtUtil.isTokenValid(accessToken)).thenReturn(true);
        when(blackTokenRedisService.isTokenBlacklisted(accessToken)).thenReturn(true); // 블랙리스트!

        // when
        boolean result = jwtTokenService.isAccessTokenValid(accessToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("로그아웃 (토큰 무효화) 성공")
    void invalidateTokens_success() {
        // given
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        Long memberId = 1L;

        when(jwtUtil.isTokenValid(accessToken)).thenReturn(true);
        when(jwtUtil.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtUtil.getMemberIdFromToken(refreshToken)).thenReturn(memberId);

        // when
        jwtTokenService.invalidateTokens(accessToken, refreshToken);

        // then
        verify(blackTokenRedisService, times(1)).addBlacklistedToken(accessToken);
        verify(redisTokenService, times(1)).deleteRefreshTokenByMemberId(memberId);
    }
}
