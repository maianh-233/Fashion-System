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

    @Test
    void validationErrorsUseTheSamePayloadWithFirstMessage() throws Exception {
        var target = new Object();
        var binding = new org.springframework.validation.BeanPropertyBindingResult(target, "cancel");
        binding.addError(new org.springframework.validation.FieldError("cancel", "reason", "Vui lòng nhập lý do hủy đơn"));
        var parameter = new org.springframework.core.MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("validationErrorsUseTheSamePayloadWithFirstMessage"), -1);

        ResponseEntity<Object> response = handler.handleValidation(
                new org.springframework.web.bind.MethodArgumentNotValidException(parameter, binding));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Vui lòng nhập lý do hủy đơn", body.get("message"));
        assertEquals(Map.of("reason", "Vui lòng nhập lý do hủy đơn"), body.get("errors"));
    }
}
