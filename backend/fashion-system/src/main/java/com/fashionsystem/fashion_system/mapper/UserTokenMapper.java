package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.UserTokenDto;
import com.fashionsystem.fashion_system.entity.UserToken;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link UserToken} và {@link UserTokenDto}.
 */
@Component
public class UserTokenMapper {
    public UserTokenDto toDto(UserToken entity) {
        if (entity == null) {
            return null;
        }
        UserTokenDto dto = new UserTokenDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public UserToken toEntity(UserTokenDto dto) {
        if (dto == null) {
            return null;
        }
        UserToken entity = new UserToken();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

