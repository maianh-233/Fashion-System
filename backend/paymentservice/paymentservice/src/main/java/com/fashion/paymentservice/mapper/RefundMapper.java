package com.fashion.paymentservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.fashion.paymentservice.dto.common.RefundDto;
import com.fashion.paymentservice.dto.request.CreateRefundRequest;
import com.fashion.paymentservice.entity.Payment;
import com.fashion.paymentservice.entity.Refund;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RefundMapper {

    @Mapping(source = "payment.id", target = "paymentId")
    RefundDto toDto(Refund refund);

    List<RefundDto> toDtoList(List<Refund> refunds);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "amount", source = "request.amount")
    @Mapping(target = "refundCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requestedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    Refund toEntity(
            CreateRefundRequest request,
            Payment payment
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(
            RefundDto dto,
            @MappingTarget Refund refund
    );
}