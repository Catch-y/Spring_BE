package umc.catchy.domain.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.service.OAuthService.SocialUserInfo;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.config.auth.AuthConfig;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.feign.AppleFeignClient;
import umc.catchy.infra.feign.KakaoAuthClient;
import umc.catchy.infra.feign.KakaoFeignClient;
import umc.catchy.infra.feign.dto.KakaoInfoResponse;
import umc.catchy.infra.feign.dto.KakaoInfoResponse.KakaoAccount;
import umc.catchy.infra.feign.dto.KakaoTokenResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @InjectMocks
    private OAuthService oAuthService;

    @Mock
    private AuthConfig authConfig;

    @Mock
    private KakaoFeignClient kakaoFeignClient;

    @Mock
    private KakaoAuthClient kakaoAuthClient;

    @Mock
    private AppleFeignClient appleFeignClient;

    @Test
    @DisplayName("Kakao 유저 정보 조회 성공")
    void getUserInfo_kakao_success() {
        // given
        String accessToken = "kakao-access-token";
        KakaoInfoResponse mockResponse = new KakaoInfoResponse(
                12345L,
                new KakaoAccount("kakao@test.com")
        );

        when(kakaoFeignClient.getUserInfo(anyString()))
                .thenReturn(mockResponse);

        // when
        SocialUserInfo userInfo = oAuthService.getUserInfo(accessToken, SocialType.KAKAO);

        // then
        assertThat(userInfo.providerId()).isEqualTo("12345");
        assertThat(userInfo.email()).isEqualTo("kakao@test.com");
    }

    @Test
    @DisplayName("Kakao 유저 정보 조회 실패 - API 에러")
    void getUserInfo_kakao_fail() {
        // given
        when(kakaoFeignClient.getUserInfo(anyString()))
                .thenThrow(new RuntimeException("Kakao API Error"));

        // when & then
        assertThatThrownBy(() -> oAuthService.getUserInfo("token", SocialType.KAKAO))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("Kakao 액세스 토큰 발급 성공")
    void getKakaoAccessToken_success() {
        // given
        String authCode = "auth-code";
        KakaoTokenResponse mockTokenResponse = new KakaoTokenResponse(
                "access-token", "refresh-token", 3600
        );

        when(kakaoAuthClient.getAccessToken(any(), any(), any(), any(), any()))
                .thenReturn(mockTokenResponse);

        // when
        String accessToken = oAuthService.getKakaoAccessToken(authCode);

        // then
        assertThat(accessToken).isEqualTo("access-token");
    }

    @Test
    @DisplayName("Apple 유저 정보 조회 실패 - 토큰 형식 불일치")
    void getUserInfo_apple_fail() {
        String invalidToken = "invalid-token";

        assertThatThrownBy(() -> oAuthService.getUserInfo(invalidToken, SocialType.APPLE))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
    }
}
