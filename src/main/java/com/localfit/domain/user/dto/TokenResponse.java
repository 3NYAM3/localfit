package com.localfit.domain.user.dto;

import lombok.Getter;

/**
 * 로그인/토큰재발급 응답
 */
@Getter
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
