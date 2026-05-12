package com.fashion.paymentservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.fashion.paymentservice.dto.common.PaymentWebhookLogDto;
import com.fashion.paymentservice.dto.request.ProcessWebhookRequest;
import com.fashion.paymentservice.entity.PaymentWebhookLog;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentWebhookLogMapper {

    PaymentWebhookLogDto toDto(PaymentWebhookLog log);

    List<PaymentWebhookLogDto> toDtoList(List<PaymentWebhookLog> logs);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "processed", constant = "false")
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PaymentWebhookLog toEntity(ProcessWebhookRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(
            PaymentWebhookLogDto dto,
            @MappingTarget PaymentWebhookLog log
    );
}