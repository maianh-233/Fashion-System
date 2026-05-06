package com.fashion.authservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.authservice.dto.response.permission.PermissionResponse;
import com.fashion.authservice.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "groupName", source = "group.name")
    PermissionResponse toResponse(Permission permission);
}
