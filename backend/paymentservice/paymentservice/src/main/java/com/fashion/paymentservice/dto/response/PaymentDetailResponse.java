package com.fashion.paymentservice.dto.response;

import java.util.List;

import com.fashion.paymentservice.dto.common.PaymentDto;
import com.fashion.paymentservice.dto.common.PaymentTransactionDto;
import com.fashion.paymentservice.dto.common.RefundDto;

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
public class PaymentDetailResponse {

    private PaymentDto payment;

    private List<PaymentTransactionDto> transactions;

    private List<RefundDto> refunds;
}