package umc.catchy.domain.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.minidev.json.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.domain.member.service.MemberAccountFacade;
import umc.catchy.domain.member.service.OAuthService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.common.response.status.SuccessStatus;

import java.io.IOException;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class AuthController {

    private final MemberAccountFacade memberAccountFacade;
    private final OAuthService oAuthService;

    @PostMapping(value = "/signup/{platform}", consumes = "multipart/form-data")
    @Operation(summary = "소셜 회원가입 API", description = "소셜 로그인 후 계정이 없다면 진행")
    public ResponseEntity<BaseResponse<SignUpResponse>> signUp(
            @Parameter(name = "platform", description = "소셜 로그인 플랫폼 (KAKAO 또는 APPLE)", required = true, in = ParameterIn.PATH)
            @PathVariable("platform") String platform,
            @RequestPart("info") @Valid SignUpRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

        SocialType socialType;

        try {
            socialType = SocialType.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.onFailure(ErrorStatus.PLATFORM_BAD_REQUEST));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.onSuccess(SuccessStatus._CREATED, memberAccountFacade.signUp(request, profileImage, socialType)));
    }

    @PostMapping("/login/{platform}")
    @Operation(summary = "소셜 로그인 API", description = "카카오/애플 계정의 존재 여부 확인")
    public ResponseEntity<BaseResponse<LoginResponse>> login(
            @Parameter(name = "platform", description = "소셜 로그인 플랫폼 (KAKAO 또는 APPLE)", required = true, in = ParameterIn.PATH)
            @PathVariable("platform") String platform,
            @RequestBody @Valid LoginRequest request) {

        SocialType socialType;

        try {
            socialType = SocialType.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(BaseResponse.onFailure(ErrorStatus.PLATFORM_BAD_REQUEST));
        }

        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, memberAccountFacade.login(request, socialType)));
    }

    @PostMapping("/callback/apple")
    @Operation(summary = "애플 로그인 redirect url", description = "회원가입 시 필요한 정보를 응답")
    public ResponseEntity<BaseResponse<AppleLoginResponse>> appleCallback(
            @RequestParam("code") String code,
            @RequestParam("id_token") String idToken) {

        AppleLoginResponse response = AppleLoginResponse.of(code, idToken);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @GetMapping("/callback/kakao")
    @Operation(summary = "카카오 로그인 redirect url", description = "회원가입 시 필요한 정보를 응답")
    public ResponseEntity<BaseResponse<KakaoLoginResponse>> kakaoCallback(
            @RequestParam("code") String code) {

        KakaoLoginResponse response = KakaoLoginResponse.of(code);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, response));
    }

    @GetMapping("/reissue")
    @Operation(summary = "토큰 검사 및 재발급 API", description = "refresh token 검사 후 accessToken 재발급")
    public ResponseEntity<BaseResponse<ReIssueTokenResponse>> reIssue() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.onSuccess(SuccessStatus._CREATED, memberAccountFacade.reIssueRefreshToken()));
    }

    @GetMapping("/token/kakao")
    @Operation(summary = "인가코드를 통해 카카오 액세스 토큰 받아오기", description = "프론트 테스트용 API")
    public ResponseEntity<BaseResponse<String>> getAccessToken(@RequestParam("code") String code) {
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, oAuthService.getKakaoAccessToken(code)));
    }

    @DeleteMapping("/withdraw")
    @Operation(summary = "회원 탈퇴 API", description = "현재 로그인된 사용자 탈퇴")
    public ResponseEntity<BaseResponse<Void>> withdrawMember(
            @RequestParam(required = false) String authorizationCode) throws IOException, ParseException {

        memberAccountFacade.withdraw(authorizationCode);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }

    @PostMapping("/mypage/logout")
    @Operation(summary = "로그아웃 API", description = "사용자의 토큰을 만료시킨다.")
    public ResponseEntity<BaseResponse<Void>> logout() {
        memberAccountFacade.logout();
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }
}
