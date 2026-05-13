package com.fashion.notificationservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.notificationservice.dto.response.NotificationLogResponse;
import com.fashion.notificationservice.entity.NotificationLog;

@Mapper(config = GlobalMapperConfig.class)
public interface NotificationLogMapper {

    @Mapping(target = "notificationId",
            source = "notification.id")
    NotificationLogResponse toResponse(NotificationLog notificationLog);

    List<NotificationLogResponse> toResponseList(
            List<NotificationLog> notificationLogs
    );
}