package umc.catchy.domain.member.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.domain.member.service.MemberAccountService;
import umc.catchy.domain.member.service.OAuthService;
import umc.catchy.support.ControllerTestSupport;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest extends ControllerTestSupport {

    @MockitoBean
    private MemberAccountService memberAccountService;

    @MockitoBean
    private OAuthService oAuthService;

    @Test
    @DisplayName("소셜 로그인 - 성공 (KAKAO)")
    void login_success() throws Exception {
        // given
        String platform = "KAKAO";
        LoginRequest request = new LoginRequest("valid-access-token");

        LoginResponse response = new LoginResponse(
                1L, "kakao_123", "test@test.com", "닉네임",
                LocalDateTime.now(), "new-access", "new-refresh"
        );

        when(memberAccountService.login(any(LoginRequest.class), eq(SocialType.KAKAO)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/member/login/{platform}", platform)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("new-access"));
    }

    @Test
    @DisplayName("소셜 로그인 - 실패 (잘못된 플랫폼)")
    void login_fail_invalid_platform() throws Exception {
        // given
        String platform = "GOOGLE";
        LoginRequest request = new LoginRequest("token");

        // when & then
        mockMvc.perform(post("/member/login/{platform}", platform)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("SOCIAL400"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 소셜 플랫폼입니다. (KAKAO 또는 APPLE만 허용)"));
    }

    @Test
    @DisplayName("소셜 회원가입 - 성공")
    void signup_success() throws Exception {
        // given
        String platform = "KAKAO";

        SignUpRequest signUpRequest = new SignUpRequest(
                "valid-access-token",
                null,
                "테스트"
        );

        MockMultipartFile info = new MockMultipartFile(
                "info",
                "",
                "application/json",
                objectMapper.writeValueAsString(signUpRequest).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "profile.jpg",
                "image/jpeg",
                "fake-image-binary".getBytes()
        );

        SignUpResponse response = new SignUpResponse(
                1L,
                "kakao_12345",
                "test@test.com",
                "테스트닉네임",
                "https://s3.aws.com/profile.jpg",
                LocalDateTime.now(),
                "new-access-token",
                "new-refresh-token",
                null
        );

        when(memberAccountService.signUp(any(SignUpRequest.class), any(), eq(SocialType.KAKAO)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/member/signup/{platform}", platform)
                        .file(info)
                        .file(profileImage)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.id").value(1L))
                .andExpect(jsonPath("$.result.email").value("test@test.com"))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("토큰 재발급 - 성공")
    void reissue_success() throws Exception {
        // given
        ReIssueTokenResponse response = new ReIssueTokenResponse("new-access-token", "new-refresh-token");

        when(memberAccountService.reIssueRefreshToken()).thenReturn(response);

        // when & then
        mockMvc.perform(get("/member/reissue")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("회원 탈퇴 - 성공")
    void withdraw_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/member/withdraw")
                        .param("authorizationCode", "apple-auth-code")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() throws Exception {
        // when & then
        mockMvc.perform(post("/member/mypage/logout")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}
