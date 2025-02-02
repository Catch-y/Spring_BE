package umc.catchy.global.error.exception;

import lombok.Getter;
import umc.catchy.global.common.response.status.ErrorStatus;

@Getter
public class ResultEmptyListException extends RuntimeException {
    private final ErrorStatus errorStatus;

    public ResultEmptyListException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
   }
}

