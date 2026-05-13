package com.fashion.notificationservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.fashion.notificationservice.dto.request.CreateNotificationRequest;
import com.fashion.notificationservice.dto.response.NotificationResponse;
import com.fashion.notificationservice.entity.Notification;

@Mapper(config = GlobalMapperConfig.class)
public interface NotificationMapper {

    Notification toEntity(CreateNotificationRequest request);

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);

    @BeanMapping(nullValuePropertyMappingStrategy =
            NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(
            CreateNotificationRequest request,
            @MappingTarget Notification notification
    );
}