package umc.catchy.global.common.response.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import umc.catchy.global.common.response.code.ErrorReasonDTO;
import umc.catchy.global.common.response.code.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // 기본 에러
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    INVALID_REQUEST_INFO(HttpStatus.BAD_REQUEST, "COMMON404", "요청된 정보가 올바르지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COMMON405", "유효성 검증에 실패했습니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON405", "유효하지 않은 파라미터입니다."),


    // 소셜 로그인 관련 에러
    PLATFORM_BAD_REQUEST(HttpStatus.BAD_REQUEST, "SOCIAL400", "유효하지 않은 소셜 플랫폼입니다. (KAKAO 또는 APPLE만 허용)"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SOCIAL404", "해당 소셜 플랫폼에 회원정보가 없습니다."),
    PROVIDER_ID_DUPLICATE(HttpStatus.CONFLICT, "MEMBER409", "이미 회원가입된 providerId입니다."),
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "MEMBER409", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "MEMBER409", "이미 사용 중인 닉네임입니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder().message(message).code(code).isSuccess(false).build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
