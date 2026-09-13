package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.GoodsReceiptItemMapper;
import com.fashionsystem.fashion_system.mapper.GoodsReceiptMapper;
import com.fashionsystem.fashion_system.repository.GoodsReceiptItemRepository;
import com.fashionsystem.fashion_system.repository.GoodsReceiptRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Catches cross-Store receipt access and request actor impersonation. */
@ExtendWith(MockitoExtension.class)
class GoodsReceiptScopeTest {
    @Mock GoodsReceiptRepository receiptRepository;
    @Mock GoodsReceiptItemRepository itemRepository;
    @Mock StoreRepository storeRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock UserRepository userRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductRepository productRepository;
    @Mock GoodsReceiptItemMapper itemMapper;
    @Mock InventoryService inventoryService;
    @Mock AuthorizationService authorizationService;
    @Mock UserScopeService userScopeService;

    private GoodsReceiptService service;
    private UUID actorId;
    private UUID storeA;
    private UUID storeB;

    @BeforeEach
    void setUp() {
        service = new GoodsReceiptService(receiptRepository, itemRepository, storeRepository,
                supplierRepository, userRepository, variantRepository, productRepository,
                new GoodsReceiptMapper(), itemMapper, inventoryService,
                authorizationService, userScopeService);
        actorId = UUID.randomUUID();
        storeA = UUID.randomUUID();
        storeB = UUID.randomUUID();
    }

    @Test
    void createRejectsAnotherStoreBeforePersistence() {
        allow("IMPORT_RECEIPT_CREATE");
        when(userScopeService.resolveStoreId(actorId, storeB))
                .thenThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"));

        assertThatThrownBy(() -> service.create(actorId, request(storeB)))
                .isInstanceOf(BusinessException.class);

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void createUsesAuthenticatedActorInsteadOfReceivedByFromRequest() {
        allow("IMPORT_RECEIPT_CREATE");
        when(userScopeService.resolveStoreId(actorId, storeA)).thenReturn(storeA);
        when(storeRepository.existsById(storeA)).thenReturn(true);
        when(receiptRepository.save(any(GoodsReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        GoodsReceiptDto request = request(storeA);
        request.setReceivedBy(UUID.randomUUID());

        GoodsReceiptDto result = service.create(actorId, request);

        assertThat(result.getStoreId()).isEqualTo(storeA);
        assertThat(result.getReceivedBy()).isEqualTo(actorId);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void detailChecksPersistedStoreOwnership() {
        allow("IMPORT_RECEIPT_VIEW");
        UUID receiptId = UUID.randomUUID();
        GoodsReceipt receipt = GoodsReceipt.builder().id(receiptId).storeId(storeB).build();
        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        org.mockito.Mockito.doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
                .when(userScopeService).requireStoreAccess(actorId, storeB);

        assertThatThrownBy(() -> service.getById(actorId, receiptId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveChecksPersistedStoreBeforeInventoryMutation() {
        allow("IMPORT_RECEIPT_APPROVE");
        UUID receiptId = UUID.randomUUID();
        GoodsReceipt receipt = GoodsReceipt.builder()
                .id(receiptId).storeId(storeB).status("PENDING").build();
        when(receiptRepository.findByIdForUpdate(receiptId)).thenReturn(Optional.of(receipt));
        org.mockito.Mockito.doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
                .when(userScopeService).requireStoreAccess(actorId, storeB);

        assertThatThrownBy(() -> service.approve(actorId, receiptId))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(inventoryService);
    }

    private void allow(String permission) {
        when(authorizationService.hasPermission(actorId, permission)).thenReturn(true);
    }

    private GoodsReceiptDto request(UUID storeId) {
        return GoodsReceiptDto.builder()
                .receiptCode(" gr-001 ")
                .storeId(storeId)
                .status("APPROVED")
                .totalQuantity(999)
                .build();
    }
}
