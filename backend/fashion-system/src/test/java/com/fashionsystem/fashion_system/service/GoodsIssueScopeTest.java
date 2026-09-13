package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.entity.GoodsIssue;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.GoodsIssueItemMapper;
import com.fashionsystem.fashion_system.mapper.GoodsIssueMapper;
import com.fashionsystem.fashion_system.repository.GoodsIssueItemRepository;
import com.fashionsystem.fashion_system.repository.GoodsIssueRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StockReservationRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Catches cross-Store issue access before stock or document mutation. */
@ExtendWith(MockitoExtension.class)
class GoodsIssueScopeTest {
    @Mock GoodsIssueRepository issueRepository;
    @Mock GoodsIssueItemRepository itemRepository;
    @Mock StoreRepository storeRepository;
    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock ProductRepository productRepository;
    @Mock StockReservationRepository reservationRepository;
    @Mock GoodsIssueItemMapper itemMapper;
    @Mock InventoryService inventoryService;
    @Mock AuthorizationService authorizationService;
    @Mock UserScopeService userScopeService;

    private GoodsIssueService service;
    private UUID actorId;
    private UUID storeB;

    @BeforeEach
    void setUp() {
        service = new GoodsIssueService(issueRepository, itemRepository, storeRepository,
                orderRepository, userRepository, variantRepository, productRepository,
                reservationRepository, new GoodsIssueMapper(), itemMapper, inventoryService,
                authorizationService, userScopeService);
        actorId = UUID.randomUUID();
        storeB = UUID.randomUUID();
    }

    @Test
    void createRejectsAnotherStoreBeforePersistence() {
        allow("EXPORT_RECEIPT_CREATE");
        when(userScopeService.resolveStoreId(actorId, storeB))
                .thenThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"));

        assertThatThrownBy(() -> service.create(actorId, request()))
                .isInstanceOf(BusinessException.class);
        verify(issueRepository, never()).save(any());
    }

    @Test
    void detailChecksPersistedStoreOwnership() {
        allow("EXPORT_RECEIPT_VIEW");
        UUID issueId = UUID.randomUUID();
        when(issueRepository.findById(issueId)).thenReturn(Optional.of(
                GoodsIssue.builder().id(issueId).storeId(storeB).build()));
        org.mockito.Mockito.doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
                .when(userScopeService).requireStoreAccess(actorId, storeB);

        assertThatThrownBy(() -> service.getById(actorId, issueId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveChecksPersistedStoreBeforeInventoryMutation() {
        allow("EXPORT_RECEIPT_APPROVE");
        UUID issueId = UUID.randomUUID();
        when(issueRepository.findByIdForUpdate(issueId)).thenReturn(Optional.of(
                GoodsIssue.builder().id(issueId).storeId(storeB).status("PENDING").build()));
        org.mockito.Mockito.doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
                .when(userScopeService).requireStoreAccess(actorId, storeB);

        assertThatThrownBy(() -> service.approve(actorId, issueId))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(inventoryService);
    }

    private void allow(String permission) {
        when(authorizationService.hasPermission(actorId, permission)).thenReturn(true);
    }

    private GoodsIssueDto request() {
        return GoodsIssueDto.builder()
                .issueCode(" gi-001 ")
                .storeId(storeB)
                .issueType("SALE")
                .build();
    }
}
