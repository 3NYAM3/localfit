package com.localfit.global.exception;

import com.localfit.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 비즈니스 예외 처리 - ErrorCode에 정의된 상태코드와 메시지로 응답 */
    @SuppressWarnings("unused")
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.error("[CustomException] code: {}, message: {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode().getStatus().value(), e.getMessage()));
    }

    /** @Valid 검증 실패 처리 - 첫 번째 필드 오류 메시지를 400으로 응답 */
    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");

        log.warn("[Validation] {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }

    /** 예상치 못한 예외 최종 처리 - NPE, 라이브러리 예외 등을 500으로 응답 */
    @SuppressWarnings("unused")
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Exception] message: {}", e.getMessage(), e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다."));
    }
}
