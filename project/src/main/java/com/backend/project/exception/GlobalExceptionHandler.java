package com.backend.project.exception;

import com.backend.project.dto.ApiErrorResponse;
import com.backend.project.dto.ApiFieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("ApiException: code={}, status={}, message={}", ec.getCode(), ec.getHttpStatus(), e.getMessage());
        return ResponseEntity.status(ec.getHttpStatus())
                .body(build(ec, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ApiFieldError> fieldErrors = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.add(new ApiFieldError(
                fe.getField(),
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid")));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(build(ErrorCode.VALIDATION_FAILED, fieldErrors));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(UsernameNotFoundException e) {
        log.warn("UsernameNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.USER_NOT_FOUND.getHttpStatus())
                .body(build(ErrorCode.USER_NOT_FOUND));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(build(ErrorCode.BAD_REQUEST, e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(build(ErrorCode.INTERNAL_ERROR));
    }

    private static ApiErrorResponse build(ErrorCode errorCode, String message, List<ApiFieldError> fieldErrors) {
        List<ApiFieldError> fields = fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors;
        return new ApiErrorResponse(Instant.now(), errorCode.getCode(), message, fields);
    }

    private static ApiErrorResponse build(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    /** 기본 메시지는 항상 {@link ErrorCode#getDefaultMessage()}만 사용 (응답 문구 단일 출처). */
    private static ApiErrorResponse build(ErrorCode errorCode) {
        return build(errorCode, errorCode.getDefaultMessage(), null);
    }

    private static ApiErrorResponse build(ErrorCode errorCode, List<ApiFieldError> fieldErrors) {
        return build(errorCode, errorCode.getDefaultMessage(), fieldErrors);
    }
}
