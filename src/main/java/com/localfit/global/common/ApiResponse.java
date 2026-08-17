package com.localfit.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 포맷.
 * 프론트엔드(React)가 항상 동일한 구조({status, message, data})로 응답을 받을 수 있게 하기 위함.
 */

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    //성공응답-기본메시지
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    //성공응답- 커스텀메시지 포함
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    //실패응답
    public static ApiResponse<Void> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
