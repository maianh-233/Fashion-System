package com.fashion.notificationservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.fashion.notificationservice.dto.request.CreateTemplateRequest;
import com.fashion.notificationservice.dto.request.UpdateTemplateRequest;
import com.fashion.notificationservice.dto.response.NotificationTemplateResponse;
import com.fashion.notificationservice.entity.NotificationTemplate;

@Mapper(config = GlobalMapperConfig.class)
public interface NotificationTemplateMapper {

    NotificationTemplate toEntity(CreateTemplateRequest request);

    NotificationTemplateResponse toResponse(
            NotificationTemplate notificationTemplate
    );

    List<NotificationTemplateResponse> toResponseList(
            List<NotificationTemplate> notificationTemplates
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateEntityFromRequest(
            UpdateTemplateRequest request,
            @MappingTarget NotificationTemplate notificationTemplate
    );
}