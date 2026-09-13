package com.fashionsystem.fashion_system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.entity.Store;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StoreMapperTest {
    private final StoreMapper mapper = new StoreMapper();

    @Test
    void updateKeepsGeneratedCodeAndNormalizesPhone() {
        Store entity = Store.builder().code("CH20260908ABCDEF12").build();
        StoreDto request = StoreDto.builder()
                .code("CLIENT-CANNOT-CHANGE")
                .name("Lunaria Test")
                .phone("+84 90 123 4567")
                .latitude(new BigDecimal("10.123456"))
                .longitude(new BigDecimal("106.123456"))
                .active(true)
                .build();

        mapper.updateEntity(request, entity);

        assertEquals("CH20260908ABCDEF12", entity.getCode());
        assertEquals("0901234567", entity.getPhone());
    }
}
