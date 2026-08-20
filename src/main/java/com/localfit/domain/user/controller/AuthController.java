package com.localfit.domain.user.controller;

import com.localfit.domain.user.dto.RefreshRequest;
import com.localfit.domain.user.dto.SignInRequest;
import com.localfit.domain.user.dto.SignupRequest;
import com.localfit.domain.user.dto.TokenResponse;
import com.localfit.domain.user.service.AuthService;
import com.localfit.global.common.ApiResponse;
import com.localfit.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    //회원가입
    @PostMapping("/api/auth/signup")
    public ApiResponse<Long> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(request);
        return ApiResponse.ok("회원가입 완료", userId);
    }

    //로그인
    @PostMapping("/api/auth/signin")
    public ApiResponse<TokenResponse> signIn(@Valid @RequestBody SignInRequest request) {
        return ApiResponse.ok(authService.signIn(request));
    }

    //rotation refresh토큰 재발급
    @PostMapping("/api/auth/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    //로그아웃
    @PostMapping("/api/auth/signout")
    public ApiResponse<Void> signOut(@AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.signOut(userDetails.getUserId());
        return ApiResponse.ok(null);
    }

    //인증동작 확인용
    @GetMapping("/api/auth/me")
    public ApiResponse<String> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(userDetails.getEmail());
    }
}
