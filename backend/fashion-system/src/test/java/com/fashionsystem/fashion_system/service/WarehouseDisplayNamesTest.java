package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.dto.InventoryTransactionDto;
import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.Supplier;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.repository.GoodsIssueRepository;
import com.fashionsystem.fashion_system.repository.GoodsReceiptRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WarehouseDisplayNamesTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final GoodsReceiptRepository receipts = mock(GoodsReceiptRepository.class);
    private final GoodsIssueRepository issues = mock(GoodsIssueRepository.class);
    private final WarehouseDisplayNames names = new WarehouseDisplayNames(users, suppliers, variants, products, receipts, issues);

    @Test
    void receiptPageGetsPeopleAndSupplierNamesFromOneLookupEach() {
        UUID creator = UUID.randomUUID(), approver = UUID.randomUUID(), supplier = UUID.randomUUID();
        when(users.findAllById(anyIterable())).thenReturn(List.of(
                User.builder().id(creator).fullName("Nguyễn An").username("an.nguyen").build(),
                User.builder().id(approver).fullName(" ").username("khoa.le").build()));
        when(suppliers.findAllById(anyIterable())).thenReturn(List.of(Supplier.builder().id(supplier).name("May Việt").build()));
        var first = GoodsReceiptDto.builder().receivedBy(creator).approvedBy(approver).supplierId(supplier).build();
        var second = GoodsReceiptDto.builder().receivedBy(creator).build();

        names.fillReceipts(List.of(first, second));

        assertThat(first.getReceivedByName()).isEqualTo("Nguyễn An");
        assertThat(first.getApprovedByName()).isEqualTo("khoa.le");
        assertThat(first.getCompletedByName()).isNull();
        assertThat(first.getSupplierName()).isEqualTo("May Việt");
        assertThat(second.getReceivedByName()).isEqualTo("Nguyễn An");
        verify(users, times(1)).findAllById(anyIterable());
    }

    @Test
    void transactionsGetActorProductAndReceiptCode() {
        UUID actor = UUID.randomUUID(), variant = UUID.randomUUID(), product = UUID.randomUUID(), receipt = UUID.randomUUID();
        when(users.findAllById(anyIterable())).thenReturn(List.of(User.builder().id(actor).fullName("Lê Khoa").build()));
        when(variants.findAllById(anyIterable())).thenReturn(List.of(ProductVariant.builder().id(variant).productId(product).sku("SKU-1").build()));
        when(products.findAllById(anyIterable())).thenReturn(List.of(Product.builder().id(product).name("Áo thun").build()));
        when(receipts.findAllById(anyIterable())).thenReturn(List.of(GoodsReceipt.builder().id(receipt).receiptCode("IMP-1").build()));
        var row = InventoryTransactionDto.builder().createdBy(actor).productVariantId(variant).importReceiptId(receipt).build();

        names.fillTransactions(List.of(row));

        assertThat(row.getCreatedByName()).isEqualTo("Lê Khoa");
        assertThat(row.getProductName()).isEqualTo("Áo thun");
        assertThat(row.getSku()).isEqualTo("SKU-1");
        assertThat(row.getReferenceCode()).isEqualTo("IMP-1");
    }
}
