package com.localfit.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러코드
 */

@Getter
public enum ErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 인증/회원
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 리프레시 토큰입니다."),

    //관심지역
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "등록되지 않은 관심지역입니다."),

    // 지역
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다."),

    // 외부 공공데이터 API
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 공공데이터 API 호출 중 오류가 발생했습니다."),
    EXTERNAL_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "외부 공공데이터 API 응답 형식이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
