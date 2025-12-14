package umc.catchy.domain.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import umc.catchy.domain.member.dto.request.NicknameRequest;
import umc.catchy.domain.member.dto.request.*;
import umc.catchy.domain.member.dto.response.*;
import umc.catchy.domain.member.service.*;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.common.response.status.SuccessStatus;
import umc.catchy.global.error.exception.GeneralException;


@Tag(name = "Profile", description = "사용자 프로필/설정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberProfileController {

    private final MemberProfileFacade memberProfileFacade;
    private final MemberAccountFacade memberAccountFacade;

    @GetMapping("/mypage")
    @Operation(summary = "프로필 조회 API", description = "현재 로그인된 사용자의 정보를 조회")
    public BaseResponse<ProfileResponse> getProfile() {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberProfileFacade.getCurrentMember());
    }

    @PostMapping("/mypage/nickname")
    @Operation(summary = "닉네임 중복 검사 API", description = "변경하려는 닉네임이 중복인지 검사")
    public BaseResponse<Void> validateNickname(@RequestBody @Valid NicknameRequest request) {
        memberProfileFacade.validateNickname(request);

        return BaseResponse.onSuccess(SuccessStatus.NICKNAME_AVAILABLE, null);
    }

    @PatchMapping("/mypage/nickname")
    @Operation(summary = "닉네임 변경 API", description = "현재 로그인된 사용자의 닉네임 변경")
    public BaseResponse<NicknameResponse> updateNickname(@RequestBody @Valid NicknameRequest request) {
        return BaseResponse.onSuccess(SuccessStatus._OK, memberProfileFacade.updateNickname(request));
    }

    @PatchMapping(value = "/mypage/profileImage", consumes = "multipart/form-data")
    @Operation(summary = "프로필 사진 변경 API", description = "현재 로그인된 사용자의 프로필 사진 변경")
    public BaseResponse<ProfileImageResponse> updateProfileImage(@RequestPart @Valid MultipartFile profileImage) {
        if (profileImage.isEmpty()) {
            throw new GeneralException(ErrorStatus.PROFILE_IMAGE_EMPTY);
        }

        return BaseResponse.onSuccess(SuccessStatus._OK, memberProfileFacade.updateProfileImage(profileImage));
    }

    @Operation(summary = "알람 여부 변경", description = "기존 토글 값을 변경합니다.")
    @PatchMapping("/alarm")
    public ResponseEntity<BaseResponse<Void>> memberToggleAppAlarmStateUpdate() {
        memberAccountFacade.toggleAppAlarm();
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }

    @Operation(summary = "FCM 토큰 갱신", description = "FCM 토큰을 갱신합니다.")
    @PatchMapping("/fcm-token")
    public ResponseEntity<BaseResponse<Void>> memberFcmTokenUpdate(@RequestBody UpdateFcmTokenRequest updateFcmTokenRequest) {
        memberAccountFacade.updateFcmToken(updateFcmTokenRequest);
        return ResponseEntity.ok(BaseResponse.onSuccess(SuccessStatus._OK, null));
    }
}
