package umc.catchy.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import umc.catchy.global.common.response.code.BaseErrorCode;
import umc.catchy.global.common.response.code.ErrorReasonDTO;

@Getter
public class GeneralException extends RuntimeException {

    private BaseErrorCode code;
    private String additionalMessage; // 추가 메시지 필드

    // 기본 생성자 (추가 메시지 없이 사용)
    public GeneralException(BaseErrorCode code) {
        super(code.getReason().getMessage());
        this.code = code;
        this.additionalMessage = null; // 추가 메시지는 null로 초기화
    }

    // 추가 메시지를 포함하는 생성자
    public GeneralException(BaseErrorCode code, String additionalMessage) {
        super(code.getReason().getMessage() + ": " + additionalMessage);
        this.code = code;
        this.additionalMessage = additionalMessage;
    }

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus() {
        return this.code.getReasonHttpStatus();
    }
}