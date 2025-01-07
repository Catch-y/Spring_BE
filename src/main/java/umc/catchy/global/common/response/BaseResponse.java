package umc.catchy.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import umc.catchy.global.common.response.code.ErrorReasonDTO;
import umc.catchy.global.common.response.status.ErrorStatus;
import umc.catchy.global.common.response.status.SuccessStatus;
import umc.catchy.global.common.response.code.BaseCode;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class BaseResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    public static <T> BaseResponse<T> onSuccess(SuccessStatus status, T data) {
        return new BaseResponse<>(true, status.getCode(), status.getMessage(), data);
    }

    public static <T> BaseResponse<T> of(BaseCode code, T result) {
        return new BaseResponse<>(
                true,
                code.getReasonHttpStatus().getCode(),
                code.getReasonHttpStatus().getMessage(),
                result);
    }

    public static <T> BaseResponse<T> onFailure(ErrorStatus errorCode, T data) {
        return new BaseResponse<>(false, errorCode.getCode(), errorCode.getMessage(), data);
    }

    public static <T> BaseResponse<T> onFailure(ErrorStatus errorCode) {
        return onFailure(errorCode, null);
    }

    public static <T> BaseResponse<T> onFailure(ErrorReasonDTO reason, T data) {
        return new BaseResponse<>(false, reason.getCode(), reason.getMessage(), data);
    }
}
