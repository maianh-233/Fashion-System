package com.fashion.paymentservice.dto.response;

import com.fashion.paymentservice.dto.common.PaymentTransactionDto;

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
public class PaymentTransactionResponse {

    private Boolean success;

    private String message;

    private PaymentTransactionDto data;
}