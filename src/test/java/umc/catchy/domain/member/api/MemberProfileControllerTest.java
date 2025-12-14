package umc.catchy.domain.member.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.request.UpdateFcmTokenRequest;
import umc.catchy.domain.member.dto.response.NicknameResponse;
import umc.catchy.domain.member.dto.response.ProfileImageResponse;
import umc.catchy.domain.member.dto.response.ProfileResponse;
import umc.catchy.domain.member.service.MemberAccountService;
import umc.catchy.domain.member.service.MemberProfileFacade;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.support.ControllerTestSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberProfileController.class)
public class MemberProfileControllerTest extends ControllerTestSupport {

    @MockitoBean
    private MemberProfileFacade memberProfileFacade;

    @MockitoBean
    private MemberAccountService memberAccountService;

    @Test
    @DisplayName("프로필 조회 - 성공")
    void getProfile_success() throws Exception {
        // given
        ProfileResponse mockResponse = new ProfileResponse(
                1L,
                "https://test-image.com/profile.jpg",
                "테스트유저"
        );

        when(memberProfileFacade.getCurrentMember())
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/member/mypage")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.profileImage").value("https://test-image.com/profile.jpg"))
                .andExpect(jsonPath("$.result.nickname").value("테스트유저"));
    }

    @Test
    @DisplayName("닉네임 중복 검사 - 성공")
    void validateNickname_success() throws Exception {
        // given
        NicknameRequest request = new NicknameRequest("새로운닉네임");

        // when & then
        mockMvc.perform(post("/member/mypage/nickname")
                .header("Authorization", testToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("NICKNAME200"));
    }

    @Test
    @DisplayName("닉네임 중복 검사 - 실패 (입력값 누락)")
    void validateNickname_fail_empty() throws Exception {
        // given
        NicknameRequest request = new NicknameRequest("");

        // when & then
        mockMvc.perform(post("/member/mypage/nickname")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result").value("닉네임은 1자 이상 8자 이하여야합니다."));
    }

    @Test
    @DisplayName("닉네임 중복 검사 - 실패 (8자 초과)")
    void validateNickname_fail_too_long() throws Exception {
        // given
        NicknameRequest request = new NicknameRequest("123456789");

        // when & then
        mockMvc.perform(post("/member/mypage/nickname")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result").value("닉네임은 1자 이상 8자 이하여야합니다."));
    }

    @Test
    @DisplayName("닉네임 변경 - 성공")
    void updateNickname_success() throws Exception {
        // given
        NicknameRequest request = new NicknameRequest("변경할닉네임");

        NicknameResponse response = new NicknameResponse(1L, "변경할닉네임");
        when(memberProfileFacade.updateNickname(any(NicknameRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(patch("/member/mypage/nickname")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.id").value(1L))
                .andExpect(jsonPath("$.result.nickname").value("변경할닉네임"));
    }

    @Test
    @DisplayName("닉네임 변경 - 실패 (이미 존재하는 닉네임)")
    void updateNickname_fail_duplicate() throws Exception {
        // given
        NicknameRequest request = new NicknameRequest("중복된닉네임");

        doThrow(new GeneralException(ErrorStatus.NICKNAME_DUPLICATE))
                .when(memberProfileFacade).updateNickname(any(NicknameRequest.class));

        // when & then
        mockMvc.perform(patch("/member/mypage/nickname")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER409"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("프로필 사진 변경 - 성공")
    void updateProfileImage_success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "profileImage",
                "test.jpg",
                "image/jpeg",
                "some-image-binary".getBytes()
        );

        ProfileImageResponse response = new ProfileImageResponse(1L, "https://s3.aws.com/new-image.jpg");
        when(memberProfileFacade.updateProfileImage(any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/member/mypage/profileImage")
                        .file(file)
                        .header("Authorization", testToken)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.id").value(1L))
                .andExpect(jsonPath("$.result.profileImage").value("https://s3.aws.com/new-image.jpg"));
    }

    @Test
    @DisplayName("프로필 사진 변경 - 실패 (빈 파일)")
    void updateProfileImage_fail_empty() throws Exception {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "profileImage",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when & then
        mockMvc.perform(multipart("/member/mypage/profileImage")
                        .file(emptyFile)
                        .header("Authorization", testToken)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER400"))
                .andExpect(jsonPath("$.message").value("프로필 이미지를 첨부해주세요."));
    }

    @Test
    @DisplayName("알림 설정 토글 - 성공")
    void toggleAlarm_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/member/alarm")
                        .header("Authorization", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"));
    }

    @Test
    @DisplayName("FCM 토큰 갱신 - 성공")
    void updateFcmToken_success() throws Exception {
        // given
        UpdateFcmTokenRequest request = new UpdateFcmTokenRequest("new-sample-fcm-token-1234");

        // when & then
        mockMvc.perform(patch("/member/fcm-token")
                        .header("Authorization", testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"));
    }
}
