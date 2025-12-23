package umc.catchy.global.error.exception;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.code.ErrorReasonDTO;
import umc.catchy.global.common.response.status.ErrorStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    private ResponseEntity<Object> createErrorResponse(
            Exception e, ErrorStatus errorStatus, String errorMessage, HttpHeaders headers, WebRequest request) {
        log.error("Exception: {}, Code: {}, Message: {}",
                e.getClass().getSimpleName(), errorStatus.getCode(), errorMessage);
        BaseResponse<Object> body = BaseResponse.onFailure(errorStatus, errorMessage);
        return super.handleExceptionInternal(e, body, headers, errorStatus.getHttpStatus(), request);
    }

    private ResponseEntity<Object> createErrorResponse(
            Exception e, ErrorReasonDTO reason, HttpHeaders headers, HttpServletRequest request) {

        // 상세 로그 출력 (Sentry가 읽기 쉽게)
        log.error("Exception: {}, Code: {}, Message: {}, Status: {}",
                e.getClass().getSimpleName(),
                reason.getCode(),
                reason.getMessage(),
                reason.getHttpStatus().value());

        // Sentry에 상세 정보 추가
        Sentry.configureScope(scope -> {
            scope.setTag("error_code", reason.getCode());
            scope.setTag("http_status", String.valueOf(reason.getHttpStatus().value()));
            scope.setTag("endpoint", request.getRequestURI());
            scope.setTag("method", request.getMethod());
            scope.setExtra("error_message", reason.getMessage());
        });

        // 500 에러만 Sentry에 전송
        if (reason.getHttpStatus().is5xxServerError()) {
            Sentry.captureException(e);
        }

        BaseResponse<Object> body = BaseResponse.onFailure(reason, null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), webRequest);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleGlobalException(Exception e, WebRequest request) {
        // 예상치 못한 에러는 무조건 Sentry에
        log.error("Unexpected exception occurred", e);

        Sentry.configureScope(scope -> {
            scope.setTag("error_type", e.getClass().getSimpleName());
            scope.setExtra("error_message", e.getMessage());
        });
        Sentry.captureException(e);

        return createErrorResponse(e, ErrorStatus._INTERNAL_SERVER_ERROR, e.getMessage(), HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleValidationException(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .reduce((first, second) -> first + ", " + second)
                .orElse("Validation error occurred");

        log.warn("Validation error: {}", errorMessage);

        return createErrorResponse(e, ErrorStatus.VALIDATION_ERROR, errorMessage, HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> handleCustomException(GeneralException e, HttpServletRequest request) {
        return createErrorResponse(e, e.getErrorReasonHttpStatus(), null, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        String errorMessage = String.join(", ", errors.values());

        log.warn("Method argument validation failed: {}", errorMessage);

        return createErrorResponse(e, ErrorStatus._BAD_REQUEST, errorMessage, headers, request);
    }

    @ExceptionHandler(ResultEmptyListException.class)
    public ResponseEntity<Object> handleResultEmptyListException(ResultEmptyListException e, WebRequest request) {
        log.info("Empty result list: {}", e.getErrorStatus().getCode());

        return ResponseEntity
                .status(e.getErrorStatus().getHttpStatus())
                .body(BaseResponse.onFailureWithEmptyList(e.getErrorStatus()));
    }
}
