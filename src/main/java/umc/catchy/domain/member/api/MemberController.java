package umc.catchy.domain.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.domain.Member;
import umc.catchy.domain.member.domain.SocialType;
import umc.catchy.domain.member.dto.request.LoginRequest;
import umc.catchy.domain.member.dto.request.SignUpRequest;
import umc.catchy.domain.member.dto.response.LoginResponse;
import umc.catchy.domain.member.dto.response.SignUpResponse;
import umc.catchy.domain.member.service.MemberService;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.common.response.status.SuccessStatus;
import umc.catchy.global.error.exception.GeneralException;

@Tag(name = "Member", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping(value = "/signup/{platform}", consumes = "multipart/form-data")
    @Operation(summary = "소셜 회원가입 API", description = "소셜 로그인 후 진행하는 회원가입")
    public BaseResponse<SignUpResponse> signUp(@Parameter(
            name = "platform",
            description = "소셜 로그인 플랫폼 (KAKAO 또는 APPLE)",
            required = true,
            in = ParameterIn.PATH
    )@PathVariable("platform") String platform, @RequestPart("info") @Valid SignUpRequest request, @RequestPart("profileImage") MultipartFile profileImage) {

        SocialType socialType;

        try {
            socialType = SocialType.valueOf(platform.toUpperCase());

        } catch (IllegalArgumentException e) {
            return BaseResponse.onFailure(ErrorStatus.PLATFORM_BAD_REQUEST);
        }


        return BaseResponse.onSuccess(SuccessStatus._CREATED, memberService.signUp(request, profileImage, socialType));
    }

    @PostMapping("/login/{platform}")
    @Operation(summary = "소셜 로그인 API",
            description = "카카오/애플 계정의 존재 여부 확인")
    public BaseResponse<LoginResponse> login(@Parameter(
            name = "platform",
            description = "소셜 로그인 플랫폼 (KAKAO 또는 APPLE)",
            required = true,
            in = ParameterIn.PATH
    )@PathVariable("platform") String platform, @RequestBody @Valid LoginRequest request) {

        SocialType socialType;

        try {
            socialType = SocialType.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BaseResponse.onFailure(ErrorStatus.PLATFORM_BAD_REQUEST);
        }

        return BaseResponse.onSuccess(SuccessStatus._OK, memberService.login(request));
    }


}
