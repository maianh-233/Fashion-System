package com.fashion.authservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fashion.authservice.dto.response.role.RoleResponse;
import com.fashion.authservice.entity.Role;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponseList(List<Role> roles);
}
