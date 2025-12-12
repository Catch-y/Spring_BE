package umc.catchy.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.LoginResponse;
import umc.catchy.domain.member.dto.response.SignUpResponse;
import umc.catchy.domain.member.dto.response.TokenPair;
import umc.catchy.domain.member.service.OAuthService.SocialUserInfo;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class MemberAccountServiceTest {

    @InjectMocks
    private MemberAccountService memberAccountService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuthService oAuthService;

    @Mock
    private JwtTokenService tokenService;

    @Mock
    private AmazonS3Manager s3Manager;

    @Mock
    private MultipartFile profileImage;

    private SignUpRequest signUpRequest;
    private LoginRequest loginRequest;
    private SocialUserInfo socialUserInfo;
    private Member testMember;
    private TokenPair tokenPair;

    @BeforeEach
    void setUp() {
        signUpRequest = new SignUpRequest(
                "kakao-access-token",
                null,
                "테스트닉네임"
        );

        loginRequest = new LoginRequest("kakao-access-token");

        socialUserInfo = new SocialUserInfo("kakao_12345", "test@test.com");

        testMember = Member.builder()
                .id(1L)
                .providerId("kakao_12345")
                .email("test@test.com")
                .nickname("테스트닉네임")
                .profileImage("https://s3.aws.com/profile.jpg")
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();

        tokenPair = new TokenPair("access-token", "refresh-token");
    }

    @Test
    @DisplayName("회원가입 성공 - KAKAO")
    void signUp_success() {
        // given
        when(oAuthService.getUserInfo("kakao-access-token", SocialType.KAKAO))
                .thenReturn(socialUserInfo);

        when(memberRepository.findByProviderId(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.empty());

        when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                .thenReturn("https://s3.aws.com/profile.jpg");

        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        when(tokenService.issueTokens(anyString(), any())).thenReturn(tokenPair);

        // when
        SignUpResponse response = memberAccountService.signUp(
                signUpRequest, profileImage, SocialType.KAKAO
        );

        // then
        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.email()).isEqualTo("test@test.com"),
                () -> assertThat(response.nickname()).isEqualTo("테스트닉네임"),
                () -> assertThat(response.accessToken()).isEqualTo("access-token"),
                () -> assertThat(response.refreshToken()).isEqualTo("refresh-token")
        );

        verify(oAuthService, times(1)).getUserInfo("kakao-access-token", SocialType.KAKAO);
        verify(memberRepository, times(1)).save(any(Member.class));
        verify(tokenService, times(1)).issueTokens(anyString(), any());
    }

    @Test
    @DisplayName("회원가입 실패 - ProviderId 중복")
    void signUp_fail_duplicateProviderId() {
        // given
        when(oAuthService.getUserInfo("kakao-access-token", SocialType.KAKAO))
                .thenReturn(socialUserInfo);

        when(memberRepository.findByProviderId("kakao_12345"))
                .thenReturn(Optional.of(testMember));

        // when & then
        assertThatThrownBy(() -> memberAccountService.signUp(
                signUpRequest, profileImage, SocialType.KAKAO
        ))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.PROVIDER_ID_DUPLICATE.getMessage());

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - Email 중복")
    void signUp_fail_duplicateEmail() {
        // given
        when(oAuthService.getUserInfo("kakao-access-token", SocialType.KAKAO))
                .thenReturn(socialUserInfo);

        when(memberRepository.findByProviderId(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testMember));

        // when & then
        assertThatThrownBy(() -> memberAccountService.signUp(
                signUpRequest, profileImage, SocialType.KAKAO
        ))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.EMAIL_DUPLICATE.getMessage());

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - Nickname 중복")
    void signUp_fail_duplicateNickname() {
        // given
        when(oAuthService.getUserInfo("kakao-access-token", SocialType.KAKAO))
                .thenReturn(socialUserInfo);

        when(memberRepository.findByProviderId(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByNickname("테스트닉네임"))
                .thenReturn(Optional.of(testMember));

        // when & then
        assertThatThrownBy(() -> memberAccountService.signUp(
                signUpRequest, profileImage, SocialType.KAKAO
        ))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.NICKNAME_DUPLICATE.getMessage());

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 성공 - APPLE with AuthorizationCode")
    void signUp_success_apple() {
        // given
        SignUpRequest appleRequest = new SignUpRequest(
                "apple-access-token",
                "apple-authorization-code",
                "애플유저"
        );

        SocialUserInfo appleSocialInfo = new SocialUserInfo("apple_67890", "apple@test.com");

        Member appleMember = Member.createMember(
                "apple_67890",
                "apple@test.com",
                "애플유저",
                null,
                SocialType.APPLE,
                FcmInfo.createFcmInfo()
        );

        when(oAuthService.getUserInfo("apple-access-token", SocialType.APPLE))
                .thenReturn(appleSocialInfo);

        when(memberRepository.findByProviderId(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.empty());

        when(memberRepository.save(any(Member.class))).thenReturn(appleMember);
        when(tokenService.issueTokens(anyString(), any())).thenReturn(tokenPair);

        // when
        SignUpResponse response = memberAccountService.signUp(
                appleRequest, null, SocialType.APPLE
        );

        // then
        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.email()).isEqualTo("apple@test.com")
        );

        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - APPLE인데 AuthorizationCode 없음")
    void signUp_fail_apple_noAuthCode() {
        // given
        SignUpRequest appleRequest = new SignUpRequest(
                "apple-access-token",
                null,
                "애플유저"
        );

        SocialUserInfo appleSocialInfo = new SocialUserInfo("apple_67890", "apple@test.com");

        when(oAuthService.getUserInfo("apple-access-token", SocialType.APPLE))
                .thenReturn(appleSocialInfo);

        when(memberRepository.findByProviderId(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.empty());

        when(memberRepository.save(any())).thenReturn(testMember);

        // when & then
        assertThatThrownBy(() -> memberAccountService.signUp(
                appleRequest, null, SocialType.APPLE
        ))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.AUTHORIZATION_CODE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        when(oAuthService.getUserInfo("kakao-access-token", SocialType.KAKAO))
                .thenReturn(socialUserInfo);

        when(memberRepository.findByEmailAndProviderId("test@test.com", "kakao_12345"))
                .thenReturn(Optional.of(testMember));

        when(tokenService.issueTokens("test@test.com", testMember.getId()))
                .thenReturn(tokenPair);

        // when
        LoginResponse response = memberAccountService.login(loginRequest, SocialType.KAKAO);

        // then
        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response.email()).isEqualTo("test@test.com"),
                () -> assertThat(response.accessToken()).isEqualTo("access-token"),
                () -> assertThat(response.refreshToken()).isEqualTo("refresh-token")
        );

        verify(oAuthService, times(1)).getUserInfo("kakao-access-token", SocialType.KAKAO);
        verify(memberRepository, times(1))
                .findByEmailAndProviderId("test@test.com", "kakao_12345");
        verify(tokenService, times(1)).issueTokens("test@test.com", testMember.getId());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 회원")
    void login_fail_memberNotFound() {
        // given
        when(oAuthService.getUserInfo("kakao-access-token", SocialType.KAKAO))
                .thenReturn(socialUserInfo);

        when(memberRepository.findByEmailAndProviderId("test@test.com", "kakao_12345"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberAccountService.login(loginRequest, SocialType.KAKAO))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.MEMBER_NOT_FOUND.getMessage());

        verify(tokenService, never()).issueTokens(anyString(), anyLong());
    }
}
