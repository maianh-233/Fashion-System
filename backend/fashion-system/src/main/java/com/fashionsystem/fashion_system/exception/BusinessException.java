package com.fashionsystem.fashion_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Exception nghiệp vụ HTTP dùng chung, tránh tạo một class cho từng lỗi nhỏ. */
public class BusinessException extends ResponseStatusException {
    public BusinessException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public BusinessException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    public static BusinessException badRequest(String reason) {
        return new BusinessException(HttpStatus.BAD_REQUEST, reason);
    }

    public static BusinessException unauthorized(String reason) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, reason);
    }

    public static BusinessException forbidden(String reason) {
        return new BusinessException(HttpStatus.FORBIDDEN, reason);
    }

    public static BusinessException conflict(String reason) {
        return new BusinessException(HttpStatus.CONFLICT, reason);
    }

    public static BusinessException notFound(String reason) {
        return new BusinessException(HttpStatus.NOT_FOUND, reason);
    }

    public static BusinessException invalidState(String reason) {
        return new BusinessException(HttpStatus.CONFLICT, reason);
    }

    public static BusinessException serviceUnavailable(String reason, Throwable cause) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, reason, cause);
    }

    public static BusinessException serviceUnavailable(String reason) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }
}
