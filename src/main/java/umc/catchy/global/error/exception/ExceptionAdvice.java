package umc.catchy.global.error.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import umc.catchy.global.common.response.BaseResponse;
import umc.catchy.global.common.response.code.ErrorReasonDTO;
import umc.catchy.global.common.response.status.ErrorStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(annotations = {RestController.class})
@RequiredArgsConstructor
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    private ResponseEntity<Object> createErrorResponse(
            Exception e, ErrorStatus errorStatus, String errorMessage, HttpHeaders headers, WebRequest request) {
        log.error("Exception: {}, Status: {}, Message: {}", e.getClass().getSimpleName(), errorStatus, errorMessage);
        BaseResponse<Object> body = BaseResponse.onFailure(errorStatus, errorMessage);
        return super.handleExceptionInternal(e, body, headers, errorStatus.getHttpStatus(), request);
    }

    private ResponseEntity<Object> createErrorResponse(
            Exception e, ErrorReasonDTO reason, HttpHeaders headers, HttpServletRequest request) {
        log.error("Exception: {}, Reason: {}", e.getClass().getSimpleName(), reason);
        BaseResponse<Object> body = BaseResponse.onFailure(reason, null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), webRequest);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleGlobalException(Exception e, WebRequest request) {
        return createErrorResponse(e, ErrorStatus._INTERNAL_SERVER_ERROR, e.getMessage(), HttpHeaders.EMPTY, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleValidationException(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .reduce((first, second) -> first + ", " + second)
                .orElse("Validation error occurred");
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
        return createErrorResponse(e, ErrorStatus._BAD_REQUEST, errorMessage, headers, request);
    }
}