package com.localfit.global.exception;

import com.localfit.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 애플리케이션 전역에서 발생하는 예외를 한 곳에서 처리하는 핸들러.
 * 각 Controller/Service에서 개별적으로 try-catch 하지 않아도,
 * 여기서 예외 타입별로 공통된 응답 형식(ApiResponse)으로 변환해준다.
 *
 * @RestControllerAdvice - 모든 @RestController에서 발생한 예외를 가로챈다.
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("[CustomException] code: {}, message: {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage()));
    }

    // 예상치 못한 모든 예외(NPE, 라이브러리 예외 등)의 최종 처리, 500으로 응답
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Exception] message: {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다."));
    }

    // @Valid검증 실패 시 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");

        log.warn("[Validation] {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }
}
