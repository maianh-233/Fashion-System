package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.entity.Supplier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.SupplierMapper;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class SupplierServiceTest {
    private SupplierRepository repository;
    private CatalogIdentityService identityService;
    private SupplierService service;

    @BeforeEach
    void setUp() {
        repository = mock(SupplierRepository.class);
        identityService = mock(CatalogIdentityService.class);
        service = new SupplierService(repository, new SupplierMapper(), identityService);
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createGeneratesImmutableCodeAndNormalizesContactData() {
        when(identityService.nextSupplierCode()).thenReturn("NCC000001");

        SupplierDto created = service.create(SupplierDto.builder()
                .code("CLIENT-CODE").name(" Nhà cung cấp A ")
                .email(" SALES@EXAMPLE.COM ").phone("+84 912-345-678")
                .status("INACTIVE").build());

        assertThat(created.getCode()).isEqualTo("NCC000001");
        assertThat(created.getName()).isEqualTo("Nhà cung cấp A");
        assertThat(created.getEmail()).isEqualTo("sales@example.com");
        assertThat(created.getPhone()).isEqualTo("+84912345678");
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsDuplicateEmailAndPhoneBeforeSaving() {
        when(identityService.nextSupplierCode()).thenReturn("NCC000002");
        when(repository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(SupplierDto.builder().name("Supplier")
                .email(" Duplicate@Example.com ").phone("0912345678").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Email");
        verify(repository, never()).saveAndFlush(any());

        when(repository.existsByEmail("duplicate@example.com")).thenReturn(false);
        when(repository.existsByPhone("0912345678")).thenReturn(true);
        assertThatThrownBy(() -> service.create(SupplierDto.builder().name("Supplier")
                .email("new@example.com").phone("0912 345 678").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("điện thoại");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updatePreservesGeneratedCodeAndChecksUniquenessExcludingCurrentSupplier() {
        UUID id = UUID.randomUUID();
        Supplier existing = Supplier.builder().id(id).code("NCC000010").name("Old")
                .email("old@example.com").phone("0900000000").status("ACTIVE").build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        SupplierDto updated = service.update(id, SupplierDto.builder().code("CLIENT-CODE")
                .name("New").email("NEW@EXAMPLE.COM").phone("0987-654-321").status("INACTIVE").build());

        assertThat(updated.getCode()).isEqualTo("NCC000010");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getPhone()).isEqualTo("0987654321");
        verify(repository).existsByEmailAndIdNot("new@example.com", id);
        verify(repository).existsByPhoneAndIdNot("0987654321", id);
    }

    @Test
    void rejectsMalformedPhoneBeforeSaving() {
        when(identityService.nextSupplierCode()).thenReturn("NCC000003");

        assertThatThrownBy(() -> service.create(SupplierDto.builder()
                .name("Supplier").phone("phone-number").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("điện thoại");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deleteIsSoftAndRestoreReturnsSupplierToActive() {
        UUID id = UUID.randomUUID();
        Supplier existing = Supplier.builder().id(id).code("NCC000020").name("Supplier")
                .status("ACTIVE").build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        assertThat(existing.getStatus()).isEqualTo("DELETED");
        verify(repository, never()).delete(any());

        SupplierDto restored = service.restore(id);

        assertThat(restored.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void allStatusDoesNotFilterSupplierList() {
        when(repository.search("", "", Pageable.unpaged())).thenReturn(Page.empty());

        service.getList(null, "ALL", Pageable.unpaged());

        verify(repository).search("", "", Pageable.unpaged());
    }
}
