package com.fashion.paymentservice.dto.response;

import com.fashion.paymentservice.dto.common.PaymentDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Boolean success;

    private String message;

    private PaymentDto data;
}