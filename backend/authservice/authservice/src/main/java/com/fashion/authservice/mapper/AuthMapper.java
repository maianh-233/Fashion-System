package com.fashion.authservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.authservice.dto.response.auth.AuthResponse;
import com.fashion.authservice.entity.User;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthMapper {

    @Mapping(target = "user", source = "user")
    AuthResponse toAuthResponse(User user,
                                String accessToken,
                                String refreshToken,
                                long expiresIn);
}
