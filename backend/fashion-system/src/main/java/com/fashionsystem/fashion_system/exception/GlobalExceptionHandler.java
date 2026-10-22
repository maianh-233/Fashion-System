package com.fashionsystem.fashion_system.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Xử lý ngoại lệ toàn cục cho ứng dụng, đảm bảo BusinessException / ResponseStatusException trả về đúng HTTP status và message thay vì bị Spring Security chuyển hướng lỗi thành 401. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode())
                .headers(ex.getHeaders())
                .body(Map.of(
                        "status", ex.getStatusCode().value(),
                        "message", message != null ? message : "Lỗi xử lý yêu cầu"
                ));
    }

    /**
     * Lỗi Jakarta Validation của request body: cùng định dạng {status, message} như lỗi nghiệp vụ, message là
     * lỗi đầu tiên để hiển thị ngay, {@code errors} liệt kê lỗi theo từng trường.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors())
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        String message = ex.getBindingResult().getAllErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", message,
                "errors", errors));
    }
}
