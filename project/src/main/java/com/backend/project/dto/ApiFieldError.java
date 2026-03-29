package com.backend.project.dto;

// 필드 단위 검증 오류.
public record ApiFieldError(String field, String message) {
}
