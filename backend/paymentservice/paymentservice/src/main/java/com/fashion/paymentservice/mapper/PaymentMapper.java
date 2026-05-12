package com.fashion.paymentservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.fashion.paymentservice.dto.common.PaymentDto;
import com.fashion.paymentservice.dto.request.CreatePaymentRequest;
import com.fashion.paymentservice.entity.Payment;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentMapper {

    PaymentDto toDto(Payment payment);

    List<PaymentDto> toDtoList(List<Payment> payments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "transactionCode", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "refunds", ignore = true)
    Payment toEntity(CreatePaymentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PaymentDto dto, @MappingTarget Payment payment);
}