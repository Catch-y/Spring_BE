package umc.catchy.domain.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.category.dto.request.CategorySurveyRequest;
import umc.catchy.domain.mapping.memberCategory.dto.response.MemberCategoryCreatedResponse;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.ProfileRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.request.StyleAndActiveTimeSurveyRequest;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.domain.member.service.MemberService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.common.response.status.SuccessStatus;

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
            @RequestPart("profileImage") MultipartFile profileImage) {

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

    @DeleteMapping("/member/withdraw/{platform}")
    @Operation(summary = "회원 탈퇴 API ", description = "현재 로그인된 사용자 탈퇴")
    public BaseResponse<Void> withdrawMember() {

        memberService.withdraw();

        return BaseResponse.onSuccess(SuccessStatus._OK, null);
    }

    @GetMapping("/reissue")
    @Operation(summary = "토큰 재발급 API", description = "refresh token을 통한 access token, refresh token 재발급")
    public BaseResponse<ReIssueTokenResponse> reIssue() {
        return BaseResponse.onSuccess(SuccessStatus._CREATED, memberService.reIssue());
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

    @PatchMapping("/mypage")
    @Operation(summary = "프로필 변경 API", description = "현재 로그인된 사용자의 프로필 변경")
    public BaseResponse<ProfileResponse> updateProfile(@RequestBody @Valid ProfileRequest request) {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.updateMember(request));
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
}
