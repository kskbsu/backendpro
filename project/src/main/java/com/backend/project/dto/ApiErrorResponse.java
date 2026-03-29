package com.backend.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

// REST 오류 공통 응답. code는 ErrorCode와 같은 문자열, fieldErrors는 검증 실패 시에만.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ApiFieldError> fieldErrors
) {
}
