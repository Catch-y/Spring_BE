package umc.catchy.domain.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.location.dto.request.LocationSurveyRequest;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.mapping.memberLocation.dto.response.MemberLocationCreatedResponse;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.request.*;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.domain.member.service.MemberService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.util.List;
import umc.catchy.global.error.exception.GeneralException;

@Tag(name = "Member", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping(value = "/signup/{platform}", consumes = "multipart/form-data")
    @Operation(summary = "소셜 회원가입 API", description = "소셜 로그인 후 계정이 없다면 진행")
    public BaseResponse<SignUpResponse> signUp(
            @Parameter(name = "platform", description = "소셜 로그인 플랫폼 (KAKAO 또는 APPLE)", required = true, in = ParameterIn.PATH)
            @PathVariable("platform") String platform,
            @RequestPart("info") @Valid SignUpRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

        SocialType socialType;

        try {
            socialType = SocialType.valueOf(platform.toUpperCase());

        } catch (IllegalArgumentException e) {
            return BaseResponse.onFailure(ErrorStatus.PLATFORM_BAD_REQUEST);
        }

        return BaseResponse.onSuccess(SuccessStatus._CREATED, memberService.signUp(request, profileImage, socialType));
    }

    @PostMapping("/login/{platform}")
    @Operation(summary = "소셜 로그인 API", description = "카카오/애플 계정의 존재 여부 확인")
    public BaseResponse<LoginResponse> login(
            @Parameter(name = "platform", description = "소셜 로그인 플랫폼 (KAKAO 또는 APPLE)", required = true, in = ParameterIn.PATH)
            @PathVariable("platform") String platform,
            @RequestBody @Valid LoginRequest request) {

        SocialType socialType;

        try {
            socialType = SocialType.valueOf(platform.toUpperCase());

        } catch (IllegalArgumentException e) {
            return BaseResponse.onFailure(ErrorStatus.PLATFORM_BAD_REQUEST);
        }

        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.login(request, socialType));
    }

    @PostMapping("/callback/apple")
    @Operation(summary = "애플 로그인 redirect url", description = "회원가입 시 필요한 정보를 응답")
    public BaseResponse<AppleLoginResponse> appleCallback(HttpServletRequest request) {

        if (request.getParameter("code") == null || request.getParameter("id_token") == null) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }

        AppleLoginResponse response = AppleLoginResponse.of(request.getParameter("code"), request.getParameter("id_token"));

        return BaseResponse.onSuccess(SuccessStatus._OK, response);
    }

    @GetMapping("/callback/kakao")
    @Operation(summary = "카카오 로그인 redirect url", description = "회원가입 시 필요한 정보를 응답")
    public BaseResponse<KakaoLoginResponse> kakaoCallback(HttpServletRequest request) {

        if (request.getParameter("code") == null) {
            throw new GeneralException(ErrorStatus.SOCIAL_MEMBER_NOT_FOUND);
        }

        KakaoLoginResponse response = KakaoLoginResponse.of(request.getParameter("code"));

        return BaseResponse.onSuccess(SuccessStatus._OK, response);
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴 API ", description = "현재 로그인된 사용자 탈퇴 / 애플 탈퇴 시 인가 코드를 입력")
    public BaseResponse<Void> withdrawMember(@RequestParam(required = false) String authorizationCode) {
        memberService.withdraw(authorizationCode);

        return BaseResponse.onSuccess(SuccessStatus._OK, null);
    }

    @GetMapping("/reissue")
    @Operation(summary = "토큰 검사 및 재발급 API", description = "refresh token 검사 후 accessToken 재발급, 만료되었다면 재로그인")
    public BaseResponse<ReIssueTokenResponse> reIssue() {
        return BaseResponse.onSuccess(SuccessStatus._CREATED, memberService.validateRefreshToken());
    }

    @GetMapping("/token/kakao")
    @Operation(summary = "인가코드를 통해 카카오 액세스 토큰 받아오기", description = "실제로는 프론트에서 액세스 토큰을 지급함")
    public BaseResponse<String> getAccessToken(String code) {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.getKakaoAccessToken(code));
    }

    @GetMapping("/mypage")
    @Operation(summary = "프로필 조회 API", description = "현재 로그인된 사용자의 정보를 조회")
    public BaseResponse<ProfileResponse> getProfile() {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.getCurrentMember());
    }

    @PostMapping("/mypage/nickname")
    @Operation(summary = "닉네임 중복 검사 API", description = "변경하려는 닉네임이 중복인지 검사")
    public BaseResponse<Void> validateNickname(@RequestBody @Valid NicknameRequest request) {
        memberService.validateNickname(request);

        return BaseResponse.onSuccess(SuccessStatus.NICKNAME_AVAILABLE, null);
    }

    @PatchMapping("/mypage/nickname")
    @Operation(summary = "닉네임 변경 API", description = "현재 로그인된 사용자의 닉네임 변경")
    public BaseResponse<NicknameResponse> updateNickname(@RequestBody @Valid NicknameRequest request) {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.updateNickname(request));
    }

    @PatchMapping(value = "/mypage/profileImage", consumes = "multipart/form-data")
    @Operation(summary = "프로필 사진 변경 API", description = "현재 로그인된 사용자의 프로필 사진 변경")
    public BaseResponse<ProfileImageResponse> updateProfileImage(@RequestPart @Valid MultipartFile profileImage) {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.updateProfileImage(profileImage));
    }

    @PostMapping("/mypage/logout")
    @Operation(summary = "로그아웃 API", description = "사용자의 토큰을 만료시킨다.")
    public BaseResponse<Void> logout() {
        memberService.logout();
        return BaseResponse.onSuccess(SuccessStatus._OK, null);
    }

    @PostMapping("/survey/category")
    @Operation(summary = "사용자 취향설문 카테고리 저장 API ", description = "사용자 취향설문 1,2단계를 저장")
    public BaseResponse<MemberCategoryCreatedResponse> createMemberCategory(
            @RequestBody CategorySurveyRequest request) {
        MemberCategoryCreatedResponse response = memberService.createMemberCategory(request);
        return BaseResponse.onSuccess(SuccessStatus._CREATED, response);
    }

    @PostMapping("/survey/styletime")
    @Operation(summary = "사용자 취향설문 참여스타일 및 활동요일,시간 저장 API ", description = "사용자 취향설문 3,4단계를 저장")
    public BaseResponse<StyleAndActiveTimeSurveyCreatedResponse> createMemberStyleTime(
            @RequestBody StyleAndActiveTimeSurveyRequest request) {
        StyleAndActiveTimeSurveyCreatedResponse response = memberService.createStyleAndActiveTimeSurvey(request);
        return BaseResponse.onSuccess(SuccessStatus._CREATED, response);
    }

    @PostMapping("/survey/location")
    @Operation(summary = "사용자 취향설문 선호지역 저장 API", description = "사용자 취향설문 5단계를 저장")
    public BaseResponse<MemberLocationCreatedResponse> createMemberLocation(@RequestBody List<LocationSurveyRequest> request) {
        MemberLocationCreatedResponse response = memberService.createMemberLocation(request);
        return BaseResponse.onSuccess(SuccessStatus._CREATED, response);
    }

    @Operation(summary = "알람 여부 변경", description = "기존 토글 값을 변경합니다.")
    @PatchMapping("/alarm")
    public ResponseEntity<BaseResponse<Void>> memberToggleAppAlarmStateUpdate() {
        memberService.toggleAppAlarm();
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }

    @Operation(summary = "FCM 토큰 갱신", description = "FCM 토큰을 갱신합니다.")
    @PatchMapping("/fcm-token")
    public ResponseEntity<BaseResponse<Void>> memberFcmTokenUpdate(@RequestBody UpdateFcmTokenRequest updateFcmTokenRequest) {
        memberService.updateFcmToken(updateFcmTokenRequest);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }
}
