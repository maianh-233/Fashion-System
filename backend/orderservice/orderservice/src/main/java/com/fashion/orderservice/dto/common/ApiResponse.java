package com.fashion.orderservice.dto.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
}
