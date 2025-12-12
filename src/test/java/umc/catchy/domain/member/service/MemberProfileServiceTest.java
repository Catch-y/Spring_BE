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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

    @InjectMocks
    private MemberProfileService memberProfileService;

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
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // when
            ProfileResponse response = memberProfileService.getCurrentMember();

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.id()).isEqualTo(1L),
                    () -> assertThat(response.nickname()).isEqualTo("테스트닉네임"),
                    () -> assertThat(response.profileImage()).isEqualTo("https://s3.aws.com/old-profile.jpg")
            );

            verify(memberRepository, times(1)).findById(1L);
        }
    }

    @Test
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() {
        // given
        NicknameRequest request = new NicknameRequest("새로운닉네임");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findByNickname("새로운닉네임")).thenReturn(Optional.empty());
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

            // when
            NicknameResponse response = memberProfileService.updateNickname(request);

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.id()).isEqualTo(1L),
                    () -> assertThat(response.nickname()).isEqualTo("새로운닉네임")
            );

            verify(memberRepository, times(1)).findByNickname("새로운닉네임");
            verify(memberRepository, times(1)).findById(1L);
        }
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 중복된 닉네임")
    void updateNickname_fail_duplicate() {
        // given
        NicknameRequest request = new NicknameRequest("중복닉네임");
        Member otherMember = Member.builder().id(2L).nickname("중복닉네임").build();

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findByNickname("중복닉네임")).thenReturn(Optional.of(otherMember));

            // when & then
            assertThatThrownBy(() -> memberProfileService.updateNickname(request))
                    .isInstanceOf(GeneralException.class)
                    .hasMessageContaining(ErrorStatus.NICKNAME_DUPLICATE.getMessage());

            verify(memberRepository, times(1)).findByNickname("중복닉네임");
            verify(memberRepository, never()).findById(any());
        }
    }

    @Test
    @DisplayName("프로필 이미지 변경 성공 - 기존 이미지 있음")
    void updateProfileImage_success_withOldImage() {
        // given
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-profile.jpg");

            // when
            ProfileImageResponse response = memberProfileService.updateProfileImage(profileImage);

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.id()).isEqualTo(1L),
                    () -> assertThat(response.profileImage()).isEqualTo("https://s3.aws.com/new-profile.jpg")
            );

            verify(s3Manager, times(1)).deleteImage("https://s3.aws.com/old-profile.jpg");
            verify(s3Manager, times(1)).uploadFile(anyString(), any(MultipartFile.class));
        }
    }

    @Test
    @DisplayName("프로필 이미지 변경 성공 - 기존 이미지 없음")
    void updateProfileImage_success_withoutOldImage() {
        // given
        Member memberWithoutImage = Member.builder()
                .id(1L)
                .providerId("kakao_12345")
                .email("test@test.com")
                .nickname("테스트닉네임")
                .profileImage(null)
                .socialType(SocialType.KAKAO)
                .fcmInfo(FcmInfo.createFcmInfo())
                .build();

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(1L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberWithoutImage));
            when(s3Manager.uploadFile(anyString(), any(MultipartFile.class)))
                    .thenReturn("https://s3.aws.com/new-profile.jpg");

            // when
            ProfileImageResponse response = memberProfileService.updateProfileImage(profileImage);

            // then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.profileImage()).isEqualTo("https://s3.aws.com/new-profile.jpg")
            );

            verify(s3Manager, never()).deleteImage(anyString());
            verify(s3Manager, times(1)).uploadFile(anyString(), any(MultipartFile.class));
        }
    }

    @Test
    @DisplayName("닉네임 중복 검사 성공")
    void validateNickname_success() {
        // given
        NicknameRequest request = new NicknameRequest("사용가능닉네임");
        when(memberRepository.findByNickname("사용가능닉네임")).thenReturn(Optional.empty());

        // when & then (예외 안 터지면 성공)
        memberProfileService.validateNickname(request);

        verify(memberRepository, times(1)).findByNickname("사용가능닉네임");
    }

    @Test
    @DisplayName("닉네임 중복 검사 실패")
    void validateNickname_fail_duplicate() {
        // given
        NicknameRequest request = new NicknameRequest("중복닉네임");
        when(memberRepository.findByNickname("중복닉네임")).thenReturn(Optional.of(testMember));

        // when & then
        assertThatThrownBy(() -> memberProfileService.validateNickname(request))
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorStatus.NICKNAME_DUPLICATE.getMessage());

        verify(memberRepository, times(1)).findByNickname("중복닉네임");
    }
}
