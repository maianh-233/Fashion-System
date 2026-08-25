package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.UserDepartmentDto;
import com.fashionsystem.fashion_system.entity.UserDepartment;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class UserDepartmentMapper {
    public UserDepartmentDto toDto(UserDepartment entity) {
        if (entity == null) return null;
        UserDepartmentDto dto = new UserDepartmentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
    public UserDepartment toEntity(UserDepartmentDto dto) {
        if (dto == null) return null;
        UserDepartment entity = new UserDepartment();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
