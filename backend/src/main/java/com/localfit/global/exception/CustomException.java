package com.localfit.global.exception;

import lombok.Getter;

/**
 * 도메인 로직에서 발생하는 비즈니스 예외
 */

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
