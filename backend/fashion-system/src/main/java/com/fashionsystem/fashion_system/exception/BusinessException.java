package com.fashionsystem.fashion_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

/** Exception nghiệp vụ HTTP dùng chung, tránh tạo một class cho từng lỗi nhỏ. */
public class BusinessException extends ResponseStatusException {
    private final Long retryAfterSeconds;

    public BusinessException(HttpStatus status, String reason) {
        super(status, reason);
        this.retryAfterSeconds = null;
    }

    public BusinessException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
        this.retryAfterSeconds = null;
    }

    private BusinessException(HttpStatus status, String reason, long retryAfterSeconds) {
        super(status, reason);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static BusinessException badRequest(String reason) {
        return new BusinessException(HttpStatus.BAD_REQUEST, reason);
    }

    public static BusinessException unauthorized(String reason) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, reason);
    }

    public static BusinessException tooManyRequests(String reason, long retryAfterSeconds) {
        return new BusinessException(HttpStatus.TOO_MANY_REQUESTS, reason, Math.max(1, retryAfterSeconds));
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

    @Override
    public HttpHeaders getHeaders() {
        if (retryAfterSeconds == null) return super.getHeaders();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        return headers;
    }
}
