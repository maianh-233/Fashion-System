package com.fashionsystem.fashion_system.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
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
}
