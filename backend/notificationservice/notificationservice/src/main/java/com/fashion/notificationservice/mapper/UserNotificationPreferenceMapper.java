package com.fashion.notificationservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.fashion.notificationservice.dto.request.UpdateNotificationPreferenceRequest;
import com.fashion.notificationservice.dto.response.UserNotificationPreferenceResponse;
import com.fashion.notificationservice.entity.UserNotificationPreference;

@Mapper(config = GlobalMapperConfig.class)
public interface UserNotificationPreferenceMapper {

    UserNotificationPreferenceResponse toResponse(
            UserNotificationPreference preference
    );

    List<UserNotificationPreferenceResponse> toResponseList(
            List<UserNotificationPreference> preferences
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateEntityFromRequest(
            UpdateNotificationPreferenceRequest request,
            @MappingTarget UserNotificationPreference preference
    );
}