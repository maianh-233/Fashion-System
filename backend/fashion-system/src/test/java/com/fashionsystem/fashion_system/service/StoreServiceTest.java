package com.fashionsystem.fashion_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StoreMapper;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {
    @Mock StoreRepository repository;
    @Mock StoreMapper mapper;
    private StoreService service;

    @BeforeEach
    void setUp() {
        service = new StoreService(repository, mapper);
    }

    @Test
    void createAlwaysGeneratesCodeOnServer() {
        StoreDto request = validRequest();
        request.setCode("CLIENT-CODE");
        Store entity = new Store();

        when(repository.existsByLatitudeAndLongitude(request.getLatitude(), request.getLongitude())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.existsByCode(any())).thenReturn(false);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenAnswer(ignored -> StoreDto.builder().code(entity.getCode()).build());

        StoreDto created = service.create(request);

        assertNotEquals("CLIENT-CODE", created.getCode());
        assertTrue(created.getCode().matches("CH\\d{8}[A-F0-9]{8}"));
    }

    @Test
    void createRejectsCoordinatesUsedByAnotherStore() {
        StoreDto request = validRequest();
        when(repository.existsByLatitudeAndLongitude(request.getLatitude(), request.getLongitude())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(409, exception.getStatusCode().value());
        verify(repository, never()).saveAndFlush(any());
    }

    private StoreDto validRequest() {
        return StoreDto.builder()
                .name("Lunaria Test")
                .phone("0901234567")
                .latitude(new BigDecimal("10.123456"))
                .longitude(new BigDecimal("106.123456"))
                .active(true)
                .build();
    }
}
