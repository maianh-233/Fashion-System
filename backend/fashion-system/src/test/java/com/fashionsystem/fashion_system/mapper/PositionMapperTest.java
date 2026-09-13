package com.fashionsystem.fashion_system.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.entity.Position;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PositionMapperTest {

    @Test
    void toDtoIncludesHierarchyAndSalaryRange() {
        Position position = Position.builder()
                .id(UUID.randomUUID())
                .departmentId(UUID.randomUUID())
                .code("LEAD")
                .name("Trưởng nhóm")
                .hierarchyLevel(3)
                .minSalary(12_000_000L)
                .maxSalary(18_000_000L)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        PositionDto dto = new PositionMapper().toDto(position, null);

        assertThat(dto.getHierarchyLevel()).isEqualTo(3);
        assertThat(dto.getMinSalary()).isEqualTo(12_000_000L);
        assertThat(dto.getMaxSalary()).isEqualTo(18_000_000L);
    }
}
