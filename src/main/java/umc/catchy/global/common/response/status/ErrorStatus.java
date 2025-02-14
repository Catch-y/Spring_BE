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
    SOCIAL_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "SOCIAL404", "해당 소셜 플랫폼에 회원정보가 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "존재하지 않는 사용자 입니다."),
    PROVIDER_ID_DUPLICATE(HttpStatus.CONFLICT, "MEMBER409", "이미 회원가입된 providerId입니다."),
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "MEMBER409", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "MEMBER409", "이미 사용 중인 닉네임입니다."),
    APPLE_WITHDRAW_FAILED(HttpStatus.UNAUTHORIZED, "MEMBER401", "애플 회원탈퇴에 실패하였습니다."),
    AUTHORIZATION_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "SOCIAL404", "애플 회원가입 시 인가코드 입력은 필수입니다."),
    AUTHORIZATION_CODE_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "SOCIAL401", "인가코드가 만료되었습니다."),

    //장소 관련 에러
    PLACE_REVIEW_INVALID_MEMBER(HttpStatus.BAD_REQUEST, "PLACE_REVIEW_MEMBER400", "해당 멤버는 장소 리뷰를 달 수 있는 권한이 없습니다."),
    PLACE_VISIT_INVALID_MEMBER(HttpStatus.BAD_REQUEST, "PLACE_VISIT_MEMBER400", "해당 멤버는 장소 방문 체크를 할 수 있는 권한이 없습니다."),
    PLACE_VISIT_ALREADY_CHECK(HttpStatus.CONFLICT, "PLACE_VISIT_MEMBER409", "이미 방문 체크를 한 장소입니다."),
    PLACE_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND,"PLACE_REVIEW404","작성된 리뷰가 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE404", "해당 장소를 찾을 수 없습니다."),
    SEARCH_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "SEARCH_PLACE404", "찾는 장소가 없습니다."),

    // 카테고리 관련 에러
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "CATEGORY400", "존재하지 않는 카테고리입니다."),
    PLACE_CATEGORY_EXIST(HttpStatus.CONFLICT, "PLACE_CATEGORY409", "해당 장소에 이미 카테고리가 존재합니다."),

    //코스 관련 에러
    COURSE_REVIEW_INVALID_MEMBER(HttpStatus.BAD_REQUEST, "COURSE_REVIEW_MEMBER400", "해당 멤버는 코스 리뷰를 달 수 있는 권한이 없습니다."),
    COURSE_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND,"COURSE_REVIEW404", "해당 코스리뷰를 찾을 수 없습니다."),
    COURSE_INVALID_MEMBER(HttpStatus.BAD_REQUEST, "COURSE_MEMBER400", "해당 멤버는 코스를 수정하거나 삭제할 수 있는 권한이 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404", "해당 코스를 찾을 수 없습니다."),
    INVALID_COURSE_TYPE(HttpStatus.BAD_REQUEST, "COURSE_TYPE400", "코스타입 입력이 잘못되었습니다."),
    COURSE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,"COURSE_MEMBER404","해당 코스가 사용자코스 그룹에 속해 있지 않습니다."),

    // 그룹 관련 에러
    GROUP_INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "GROUP400", "유효하지 않은 초대 코드입니다."),
    GROUP_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "GROUP409", "이미 그룹에 가입된 회원입니다."),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404", "그룹을 찾을 수 없습니다."),
    GROUP_MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "GROUP_MEMBER404", "사용자가 그룹에 속해 있지 않습니다."),

    // 토큰 관련 에러
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "TOKEN400", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN401", "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "TOKEN402", "지원하지 않는 형식의 토큰입니다."),
    NOT_FOUND_TOKEN(HttpStatus.NOT_FOUND, "TOKEN404", "토큰의 클레임이 비어있습니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "BLACKLIST_TOKEN", "만료된 액세스토큰 입니다."),

    // 투표 관련 에러
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE404", "해당 카테고리를 찾을 수 없습니다."),
    INVALID_CATEGORY_SELECTION(HttpStatus.BAD_REQUEST, "VOTE400", "선택한 카테고리가 유효하지 않습니다."),
    CATEGORY_SELECTION_INSUFFICIENT(HttpStatus.BAD_REQUEST, "VOTE401", "최소 두 개 이상의 카테고리를 선택해야 합니다."),
    CATEGORY_ALREADY_VOTED(HttpStatus.CONFLICT, "VOTE409", "이미 해당 투표에 참여했습니다."),
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE402", "해당 투표를 찾을 수 없습니다."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "VOTE403", "이미 해당 장소에 투표했습니다."),
    PLACE_VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "VOTE406", "해당 장소 투표 정보를 찾을 수 없습니다."),
    VOTE_NOT_BELONG_TO_GROUP(HttpStatus.NOT_FOUND, "VOTE404", "해당 투표는 요청한 그룹에 속하지 않습니다."),
    VOTE_ALREADY_COMPLETED(HttpStatus.NOT_FOUND, "VOTE409", "해당 투표는 완료되었습니다."),

    // gpt 관련 에러
    JSON_PARSING_ERROR(HttpStatus.NOT_FOUND, "GPT404", "json 파싱 에러"),
    IMAGE_GENERATION_ERROR(HttpStatus.BAD_REQUEST, "GPT402", "이미지 생성에 실패하였습니다."),
    GPT_API_CALL_FAILED(HttpStatus.NOT_FOUND, "GPT404", "GPT 호출에 실패했습니다."),

    //리뷰 관련 에러
    REVIEW_DELETE_INVALID(HttpStatus.BAD_REQUEST, "REVIEW400", "해당 멤버는 리뷰 삭제 권한이 없습니다."),
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
