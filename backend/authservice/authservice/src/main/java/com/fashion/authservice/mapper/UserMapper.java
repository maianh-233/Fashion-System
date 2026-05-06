package com.fashion.authservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.authservice.dto.request.user.CreateUserRequest;
import com.fashion.authservice.dto.response.user.UserResponse;
import com.fashion.authservice.entity.User;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    User toEntity(CreateUserRequest request);
}
