package com.localfit.global.exception;

import lombok.Getter;

/**
 * 서비스 전역에서 사용하는 커스텀 예외.
 * ErrorCode를 함께 담아서, GlobalExceptionHandler가 이 예외를 잡았을 때
 * 어떤 상태코드/메시지로 응답할지 일관되게 처리할 수 있게 한다.
 *
 * 사용 예: throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
 */

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
