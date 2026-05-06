package com.fashion.customerservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.customerservice.dto.response.ActivityLogResponse;
import com.fashion.customerservice.entity.CustomerActivityLog;


@Mapper(config = MapperCentralConfig.class)
public interface ActivityLogMapper {

    @Mapping(target = "metadata", expression = "java(entity.getMetadata() != null ? entity.getMetadata() : null)")
    ActivityLogResponse toResponse(CustomerActivityLog entity);

    List<ActivityLogResponse> toResponseList(List<CustomerActivityLog> entities);
}
