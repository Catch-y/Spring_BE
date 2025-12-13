package umc.catchy.domain.member.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @InjectMocks
    private OAuthService oAuthService;

    @Mock
    private AuthConfig authConfig;

    @Test
    @DisplayName("Apple 유저 정보 조회 성공 - JWT 파싱 검증")
    void getUserInfo_apple_success() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        String providerId = "apple_12345";
        String email = "apple@test.com";

        String idToken = Jwts.builder()
                .setHeaderParam("kid", "test-key-id")
                .setSubject(providerId)
                .claim("email", email)
                .setIssuer("https://appleid.apple.com")
                .setAudience("umc.catchy.client")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact();

        SocialUserInfo userInfo = oAuthService.getUserInfo(idToken, SocialType.APPLE);

        assertAll(
                () -> assertThat(userInfo).isNotNull(),
                () -> assertThat(userInfo.providerId()).isEqualTo(providerId),
                () -> assertThat(userInfo.email()).isEqualTo(email)
        );
    }

    @Test
    @DisplayName("Apple 유저 정보 조회 실패 - 잘못된 JWT 형식")
    void getUserInfo_apple_fail_invalid_token() {
        String invalidToken = "invalid-jwt-token-garbage-value";

        assertThatThrownBy(() -> oAuthService.getUserInfo(invalidToken, SocialType.APPLE))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("getUserInfo - KAKAO 분기 및 로직 호출 확인")
    void getUserInfo_kakao_branch_test() {
        String dummyToken = "kakao-dummy-token";
        authConfig.KAKAO_INFO_URL = "http://localhost/dummy";

        assertThatThrownBy(() -> oAuthService.getUserInfo(dummyToken, SocialType.KAKAO))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
    }
}
