package com.fashionsystem.fashion_system.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatusExceptionReturnsCorrectPayloadAndStatus() {
        BusinessException exception = BusinessException.conflict("Mã phòng ban đã tồn tại");

        ResponseEntity<Object> response = handler.handleResponseStatusException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(409, body.get("status"));
        assertEquals("Mã phòng ban đã tồn tại", body.get("message"));
    }
}
