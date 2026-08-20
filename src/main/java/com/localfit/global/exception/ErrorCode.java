package com.localfit.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 서비스 전역에서 사용하는 에러 코드 목록.
 * 각 에러 상황마다 HTTP 상태코드와 사용자에게 보여줄 메시지를 미리 정의해두고,
 * CustomException을 던질 때 이 중 하나를 골라서 사용한다.
 *
 * 새로운 도메인(User, Recommendation 등)이 추가되면 관련 에러코드를 여기에 계속 추가한다.
 */

@Getter
public enum ErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // USER
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 리프레시 토큰입니다."),

    // 외부 공공데이터 API관련
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 공공데이터 API 호출 중 오류가 발생했습니다."),
    EXTERNAL_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "외부 공공데이터 API 응답 형식이 올바르지 않습니다."),

    // 지역
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 지역입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
