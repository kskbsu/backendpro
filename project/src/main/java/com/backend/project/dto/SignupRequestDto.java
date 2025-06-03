package com.backend.project.dto;

import lombok.Getter;
import lombok.Setter;

// jakarta.validation.constraints.* 등을 사용하여 유효성 검사 어노테이션 추가 가능
// 예: import jakarta.validation.constraints.NotBlank;
// 예: import jakarta.validation.constraints.Size;

@Getter
@Setter
public class SignupRequestDto {
    // @NotBlank // 예: 사용자 아이디는 비어있을 수 없습니다.
    // @Size(min = 4, max = 20) // 예: 사용자 아이디는 4자 이상 20자 이하이어야 합니다.
    private String username;

    // @NotBlank
    private String password;

    // @NotBlank
    private String nickname;

    // preferredLanguage 필드 제거
}