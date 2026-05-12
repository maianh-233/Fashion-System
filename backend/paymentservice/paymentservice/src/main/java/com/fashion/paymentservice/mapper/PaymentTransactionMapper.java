package com.fashion.paymentservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.fashion.paymentservice.dto.common.PaymentTransactionDto;
import com.fashion.paymentservice.dto.request.CreatePaymentTransactionRequest;
import com.fashion.paymentservice.entity.Payment;
import com.fashion.paymentservice.entity.PaymentTransaction;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentTransactionMapper {

    @Mapping(source = "payment.id", target = "paymentId")
    PaymentTransactionDto toDto(PaymentTransaction transaction);

    List<PaymentTransactionDto> toDtoList(List<PaymentTransaction> transactions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "status", source = "request.status")
    PaymentTransaction toEntity(
            CreatePaymentTransactionRequest request,
            Payment payment
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(
            PaymentTransactionDto dto,
            @MappingTarget PaymentTransaction transaction
    );
}