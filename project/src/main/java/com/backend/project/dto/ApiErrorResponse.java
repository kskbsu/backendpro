package com.backend.project.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ApiFieldError> fieldErrors
) {
}
