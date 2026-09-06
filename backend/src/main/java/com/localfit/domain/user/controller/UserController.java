package com.localfit.domain.user.controller;

import com.localfit.domain.user.dto.NicknameUpdateRequest;
import com.localfit.domain.user.dto.PasswordUpdateRequest;
import com.localfit.domain.user.dto.UserResponse;
import com.localfit.domain.user.service.UserService;
import com.localfit.global.common.ApiResponse;
import com.localfit.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    /**
     * 회원 정보 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 이메일, 닉네임
     */
    @GetMapping
    public ApiResponse<UserResponse> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(userService.getMyInfo(userDetails.getUserId()));
    }

    /**
     * 닉네임 변경
     *
     * @param userDetails 인증된 사용자 정보
     * @param request 새 닉네임
     * @return 변경된 회원 정보
     */
    @PatchMapping("/nickname")
    public ApiResponse<UserResponse> updateNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @Valid @RequestBody NicknameUpdateRequest request){
        return ApiResponse.ok("닉네임이 변경되었습니다.", userService.updateNickname(userDetails.getUserId(), request));
    }

    /**
     * 비밀번호를 변경한다.
     * 변경 후 기존 세션이 폐기되므로 재로그인이 필요하다.
     *
     * @param userDetails 인증된 사용자 정보
     * @param request     현재 비밀번호, 새 비밀번호
     */
    @PatchMapping("/password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(userDetails.getUserId(), request);
        return ApiResponse.ok("비밀번호가 변경되었습니다. 다시 로그인해주세요", null);
    }
}
