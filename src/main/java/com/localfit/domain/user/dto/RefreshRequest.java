package com.localfit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RefreshToken 재발급 요청
 */
@Getter
@NoArgsConstructor
public class RefreshRequest {
    @NotBlank
    private String refreshToken;
}
