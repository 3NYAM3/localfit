package com.localfit.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 포맷.
 */

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    /** 성공 응답 - 기본 메시지("OK") */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    /** 성공 응답 - 커스텀 메시지 */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /** 실패 응답 */
    public static ApiResponse<Void> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
