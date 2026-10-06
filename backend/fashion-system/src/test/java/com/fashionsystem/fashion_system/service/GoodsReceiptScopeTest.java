package com.fashionsystem.fashion_system.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class GoodsReceiptScopeTest {
 @Mock GoodsReceiptRepository repository;
 @Mock GoodsReceiptItemRepository items;
 @Mock SupplierRepository suppliers;
 @Mock ProductVariantRepository variants;
 @Mock ProductRepository products;
 @Mock InventoryService inventory;
 @Mock StoreAccessService access;
 GoodsReceiptService service;
 UUID actor = UUID.randomUUID(), store = UUID.randomUUID(), id = UUID.randomUUID(), supplier = UUID.randomUUID(), product = UUID.randomUUID(), variant = UUID.randomUUID();
 @BeforeEach void setup() {
  service = new GoodsReceiptService(repository,items,suppliers,variants,products,new GoodsReceiptMapper(),new GoodsReceiptItemMapper(),inventory,access);
 }
 private GoodsReceipt document(String status) { return GoodsReceipt.builder().id(id).storeId(store).supplierId(supplier).status(status) .build(); }
 private void locked(GoodsReceipt document) { when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(document)); }
 private void validItems() {
  when(suppliers.existsById(supplier)).thenReturn(true);
  when(items.findAllByReceiptIdOrderByCreatedAtAsc(id)).thenReturn(List.of(GoodsReceiptItem.builder().productId(product).productVariantId(variant).quantity(2).targetChannel("ONLINE") .costPrice(BigDecimal.ONE).build()));
  when(variants.findById(variant)).thenReturn(Optional.of(ProductVariant.builder().id(variant).productId(product).build()));
 }
 @Test void detailChecksPersistedStoreAndPermission() {
  when(repository.findById(id)).thenReturn(Optional.of(document("DRAFT")));
  when(access.require(actor,store,"IMPORT_RECEIPT_VIEW")).thenThrow(BusinessException.forbidden("denied"));
  assertThatThrownBy(() -> service.getById(actor,id)).isInstanceOf(BusinessException.class);
 }
 @Test void completeChecksScopeBeforeStock() {
  locked(document("CONFIRMED"));
  when(access.require(actor,store,"IMPORT_RECEIPT_COMPLETE")).thenThrow(BusinessException.forbidden("denied"));
  assertThatThrownBy(() -> service.complete(actor,id)).isInstanceOf(BusinessException.class);
  verifyNoInteractions(inventory,items);
 }
 @Test void confirmationNeverMutatesInventory() {
  var d=document("PENDING_CONFIRMATION");locked(d);validItems();
  when(repository.save(d)).thenReturn(d);
  var result=service.approve(actor,id);
  assertThat(result.getStatus()).isEqualTo("CONFIRMED");
  assertThat(result.getApprovedBy()).isEqualTo(actor);
  assertThat(result.getConfirmedAt()).isNotNull();
  verifyNoInteractions(inventory);
 }
 @Test void completionAppliesStockOnceAndRecordsActor() {
  var d=document("CONFIRMED");locked(d);validItems();when(repository.save(d)).thenReturn(d);
  var result=service.complete(actor,id);
  assertThat(result.getStatus()).isEqualTo("COMPLETED");
  assertThat(result.getCompletedBy()).isEqualTo(actor);
  assertThat(result.getCompletedAt()).isNotNull();
  assertThatThrownBy(() -> service.complete(actor,id)).isInstanceOf(BusinessException.class);
  verify(inventory,times(1)).receiveChannel(store,variant,2,"ONLINE",id,actor);
 }
 @Test void draftCannotComplete() {
  locked(document("DRAFT"));
  assertThatThrownBy(() -> service.complete(actor,id)).isInstanceOf(BusinessException.class);
  verifyNoInteractions(inventory);
 }
 @Test void confirmedCannotBeCancelledOrEdited() {
  locked(document("CONFIRMED"));
  assertThatThrownBy(() -> service.cancel(actor,id)).isInstanceOf(BusinessException.class);
  assertThatThrownBy(() -> service.update(actor,id, new GoodsReceiptRequest())).isInstanceOf(BusinessException.class);
  verifyNoInteractions(inventory);
 }
 @Test void deleteCancelsWithoutDeletingHistory() {
  var d=document("DRAFT");locked(d);when(repository.save(d)).thenReturn(d);
  service.delete(actor,id);
  assertThat(d.getStatus()).isEqualTo("CANCELLED");
  verify(repository,never()).delete(any());
 }
 @Test void updateCannotMoveReceipt() {
  locked(document("DRAFT"));
  assertThatThrownBy(() -> service.update(actor,id,GoodsReceiptRequest.builder().storeId(UUID.randomUUID()).build())).isInstanceOf(BusinessException.class);
  verify(repository,never()).save(any());
 }
 @Test void createGeneratesCodeAndCreator() {
  when(access.require(actor,store,"IMPORT_RECEIPT_CREATE")).thenReturn(store);
  when(suppliers.existsById(supplier)).thenReturn(true);
  when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
  var result=service.create(actor,GoodsReceiptRequest.builder().storeId(store).supplierId(supplier) .build());
  assertThat(result.getReceiptCode()).matches("IMP-[0-9]{8}-[a-f0-9]{32}");
  assertThat(result.getReceivedBy()).isEqualTo(actor);
  assertThat(result.getStatus()).isEqualTo("DRAFT");
 }
 @Test void itemVariantMustBelongToProduct() {
  locked(document("DRAFT"));
  when(variants.findById(variant)).thenReturn(Optional.of(ProductVariant.builder().id(variant).productId(UUID.randomUUID()).build()));
  assertThatThrownBy(() -> service.addItem(actor,id,GoodsReceiptItemRequest.builder().productId(product).productVariantId(variant).quantity(2).targetChannel("ONLINE") .costPrice(BigDecimal.ONE).build())).isInstanceOf(BusinessException.class);
  verify(items,never()).save(any());
 }

 @Test void submitRequiresItemsAndDoesNotTouchStock() {
  var d=document("DRAFT");locked(d);
  when(suppliers.existsById(supplier)).thenReturn(true);
  when(items.findAllByReceiptIdOrderByCreatedAtAsc(id)).thenReturn(List.of());
  assertThatThrownBy(() -> service.submit(actor,id)).isInstanceOf(BusinessException.class);
  assertThat(d.getStatus()).isEqualTo("DRAFT");
  verifyNoInteractions(inventory);
 }
 @Test void submitTransitionsWithoutStockMutation() {
  var d=document("DRAFT");locked(d);validItems();when(repository.save(d)).thenReturn(d);
  assertThat(service.submit(actor,id).getStatus()).isEqualTo("PENDING_CONFIRMATION");
  verifyNoInteractions(inventory);
 }
 @Test void pendingCannotBeResubmitted() {
  locked(document("PENDING_CONFIRMATION"));
  assertThatThrownBy(() -> service.submit(actor,id)).isInstanceOf(BusinessException.class);
  verifyNoInteractions(items);
 }
 @Test void createRejectsOutOfScopeStoreBeforePersistence() {
  when(access.require(actor,store,"IMPORT_RECEIPT_CREATE")).thenThrow(BusinessException.forbidden("denied"));
  assertThatThrownBy(() -> service.create(actor,GoodsReceiptRequest.builder().storeId(store).build())).isInstanceOf(BusinessException.class);
  verify(repository,never()).save(any());
 }
 @Test void inventoryFailureDoesNotMarkReceiptCompleted() {
  var d=document("CONFIRMED");locked(d);validItems();
  doThrow(BusinessException.invalidState("stock failure")).when(inventory).receiveChannel(store,variant,2,"ONLINE",id,actor);
  assertThatThrownBy(() -> service.complete(actor,id)).isInstanceOf(BusinessException.class);
  assertThat(d.getStatus()).isEqualTo("CONFIRMED");
  assertThat(d.getCompletedAt()).isNull();
  verify(repository,never()).save(any());
 }

 @Test void importRequiresSupplier() {
  assertThatThrownBy(() -> service.create(actor,GoodsReceiptRequest.builder().storeId(store).build())).isInstanceOf(BusinessException.class);
  verify(repository,never()).save(any());
 }
 @Test void negativeCostRejectedBeforeVariantLookup() {
  locked(document("DRAFT"));
  assertThatThrownBy(() -> service.addItem(actor,id,GoodsReceiptItemRequest.builder().productId(product).productVariantId(variant).quantity(1).targetChannel("ONLINE").costPrice(BigDecimal.valueOf(-1)).build())).isInstanceOf(BusinessException.class);
  verifyNoInteractions(variants);
 }

 @Test void pendingHeaderCanBeEditedWithoutChangingCreatorOrCode() {
  var d=document("PENDING_CONFIRMATION");d.setReceiptCode("ORIGINAL");d.setReceivedBy(actor);locked(d);
  when(suppliers.existsById(supplier)).thenReturn(true);
  when(repository.save(d)).thenReturn(d);
  var result=service.update(actor,id,GoodsReceiptRequest.builder().storeId(store).supplierId(supplier).note("updated") .build());
  assertThat(result.getNote()).isEqualTo("updated");
  assertThat(result.getReceiptCode()).isEqualTo("ORIGINAL");
  assertThat(result.getReceivedBy()).isEqualTo(actor);
  assertThat(result.getStatus()).isEqualTo("PENDING_CONFIRMATION");
 }
 @Test void updateRejectsUnauthorizedRequestedStoreBeforeImmutableCheck() {
  locked(document("DRAFT"));UUID other=UUID.randomUUID();
  when(access.require(actor,store,"IMPORT_RECEIPT_UPDATE")).thenReturn(store);
  var failure=BusinessException.forbidden("denied");
  when(access.require(actor,other,"IMPORT_RECEIPT_UPDATE")).thenThrow(failure);
  assertThatThrownBy(() -> service.update(actor,id,GoodsReceiptRequest.builder().storeId(other).build())).isSameAs(failure);
 }

 @Test void lineAmountOverflowRejected() {
  locked(document("DRAFT"));
  when(variants.findById(variant)).thenReturn(Optional.of(ProductVariant.builder().id(variant).productId(product).build()));
  when(products.findById(product)).thenReturn(Optional.of(Product.builder().id(product).name("Product").build()));
  assertThatThrownBy(() -> service.addItem(actor,id,GoodsReceiptItemRequest.builder().productId(product).productVariantId(variant).quantity(Integer.MAX_VALUE).targetChannel("ONLINE").costPrice(new BigDecimal("9999999999.99")).build())).isInstanceOf(BusinessException.class);
  verify(items,never()).save(any());
 }
 @Test void receiptQuantityOverflowRejected() {
  locked(document("DRAFT"));
  UUID itemId=UUID.randomUUID();
  when(items.findByIdAndReceiptId(itemId,id)).thenReturn(Optional.of(GoodsReceiptItem.builder().id(itemId).build()));
  when(items.findAllByReceiptIdOrderByCreatedAtAsc(id)).thenReturn(List.of(
      GoodsReceiptItem.builder().quantity(Integer.MAX_VALUE).total(BigDecimal.ZERO).build(),
      GoodsReceiptItem.builder().quantity(1).total(BigDecimal.ZERO).build()));
  assertThatThrownBy(() -> service.removeItem(actor,id,itemId)).isInstanceOf(BusinessException.class);
  verify(repository,never()).save(any());
 }
 @Test void oversizedPageRejectedBeforeQuery() {
  assertThatThrownBy(() -> service.getList(actor,null,store,null,null,null,null,org.springframework.data.domain.PageRequest.of(0,101))).isInstanceOf(BusinessException.class);
  verifyNoInteractions(repository);
 }
 @Test void invalidStatusFilterIsRejectedBeforeQuery() {
  when(access.require(actor,store,"IMPORT_RECEIPT_VIEW")).thenReturn(store);
  assertThatThrownBy(() -> service.getList(actor,null,store,null,"UNKNOWN",null,null,org.springframework.data.domain.PageRequest.of(0,20)))
      .isInstanceOf(BusinessException.class);
  verifyNoInteractions(repository);
 }
 @Test void dateFilterIncludesWholeToDateUsingExclusiveNextMidnight() {
  when(access.require(actor,store,"IMPORT_RECEIPT_VIEW")).thenReturn(store);
  var page=org.springframework.data.domain.PageRequest.of(0,20);
  var day=java.time.LocalDate.of(2026,10,6);
  when(repository.search("",store,null,"",day.atStartOfDay(),day.plusDays(1).atStartOfDay(),page))
      .thenReturn(org.springframework.data.domain.Page.empty());
  service.getList(actor,"",store,null,"",day,day,page);
  verify(repository).search("",store,null,"",day.atStartOfDay(),day.plusDays(1).atStartOfDay(),page);
 }
}
