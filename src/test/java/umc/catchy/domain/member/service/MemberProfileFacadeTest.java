package umc.catchy.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dao.MemberRepository;
import umc.catchy.domain.member.domain.FcmInfo;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.response.NicknameResponse;
import umc.catchy.domain.member.dto.response.ProfileImageResponse;
import umc.catchy.domain.member.dto.response.ProfileResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.error.exception.GeneralException;
import umc.catchy.global.util.SecurityUtil;
import umc.catchy.infra.aws.s3.AmazonS3Manager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberProfileFacadeTest {

    @InjectMocks
    private MemberProfileFacade memberProfileFacade;

    @Mock
    private MemberProfileCommandService memberProfileCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AmazonS3Manager s3Manager;

    @Mock
    private MultipartFile profileImage;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .providerId("kakao_12345")
                .email("test@test.com")
                .nickname("테스트닉네임")
                .profileImage("https://s3.aws.com/old-profile.jpg")
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void getCurrentMember_success() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            ProfileResponse response = memberProfileFacade.getCurrentMember();

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.nickname()).isEqualTo("테스트닉네임");
            assertThat(response.profileImage()).isEqualTo("https://s3.aws.com/old-profile.jpg");
        }
    }

    @Test
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() {
        NicknameRequest request = new NicknameRequest("새로운닉네임");

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findByNickname("새로운닉네임"))
                    .thenReturn(Optional.empty());

            when(memberProfileCommandService.updateNickname(1L, "새로운닉네임"))
                    .thenReturn(new NicknameResponse(1L, "새로운닉네임"));

            NicknameResponse response = memberProfileFacade.updateNickname(request);

            assertThat(response.nickname()).isEqualTo("새로운닉네임");
        }
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 중복")
    void updateNickname_fail_duplicate() {
        NicknameRequest request = new NicknameRequest("중복닉네임");

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findByNickname("중복닉네임"))
                    .thenReturn(Optional.of(testMember));

            assertThatThrownBy(() -> memberProfileFacade.updateNickname(request))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.NICKNAME_DUPLICATE.getMessage());
        }
    }

    @Test
    @DisplayName("프로필 이미지 변경 성공 - 기존 이미지 있음")
    void updateProfileImage_success_withOldImage() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-profile.jpg");

            when(memberProfileCommandService.updateProfileImage(
                    1L, "https://s3.aws.com/new-profile.jpg"))
                    .thenReturn(new ProfileImageResponse(
                            1L, "https://s3.aws.com/new-profile.jpg"));

            ProfileImageResponse response =
                    memberProfileFacade.updateProfileImage(profileImage);

            assertThat(response.profileImage())
                    .isEqualTo("https://s3.aws.com/new-profile.jpg");

            verify(s3Manager).deleteImage("https://s3.aws.com/old-profile.jpg");
        }
    }

    @Test
    @DisplayName("프로필 이미지 변경 성공 - 기존 이미지 없음")
    void updateProfileImage_success_withoutOldImage() {
        Member memberWithoutImage = Member.builder()
                .id(1L)
                .providerId("kakao_12345")
                .email("test@test.com")
                .nickname("테스트닉네임")
                .profileImage(null)
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);

            when(memberRepository.findById(1L))
                    .thenReturn(Optional.of(memberWithoutImage));

            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-profile.jpg");

            when(memberProfileCommandService.updateProfileImage(
                    1L, "https://s3.aws.com/new-profile.jpg"))
                    .thenReturn(new ProfileImageResponse(
                            1L, "https://s3.aws.com/new-profile.jpg"));

            ProfileImageResponse response =
                    memberProfileFacade.updateProfileImage(profileImage);

            assertThat(response.profileImage())
                    .isEqualTo("https://s3.aws.com/new-profile.jpg");

            verify(s3Manager, never()).deleteImage(anyString());
        }
    }

    @Test
    @DisplayName("닉네임 중복 검사 성공")
    void validateNickname_success() {
        NicknameRequest request = new NicknameRequest("가능");

        when(memberRepository.findByNickname("가능"))
                .thenReturn(Optional.empty());

        memberProfileFacade.validateNickname(request);
    }

    @Test
    @DisplayName("닉네임 중복 검사 실패")
    void validateNickname_fail() {
        NicknameRequest request = new NicknameRequest("중복");

        when(memberRepository.findByNickname("중복"))
                .thenReturn(Optional.of(testMember));

        assertThatThrownBy(() -> memberProfileFacade.validateNickname(request))
                .isInstanceOf(GeneralException.class);
    }
}
