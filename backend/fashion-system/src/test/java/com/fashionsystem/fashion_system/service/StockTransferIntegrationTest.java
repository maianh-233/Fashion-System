package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.StockTransferDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Điều chuyển hàng trên cơ sở dữ liệu thật; phạm vi cửa hàng được giả lập, dữ liệu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class StockTransferIntegrationTest {
    @Autowired StockTransferService transfers;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean StoreAccessService storeAccess;

    final UUID actor = UUID.randomUUID();
    final UUID shortStore = UUID.randomUUID();
    final UUID richStore = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID tee = UUID.randomUUID();
    final UUID teeL = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        String tag = product.toString().substring(0, 8);
        jdbc.update("insert into users(id, username) values (?, ?)", actor, "zqtf-" + tag);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqtf Thiếu', true)", shortStore, "ZQTF-A-" + tag);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqtf Dư', true)", richStore, "ZQTF-B-" + tag);
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqtf Áo thun', ?, 'ACTIVE', '')",
                product, "ZQTF-" + tag, "zqtf-ao-" + tag);
        variant(tee, "ZQTF-" + tag + "-M", "M");
        variant(teeL, "ZQTF-" + tag + "-L", "L");
        balance(shortStore, tee, 1, 1);
        balance(shortStore, teeL, 30, 0);
        balance(richStore, tee, 20, 0);
        balance(richStore, teeL, 2, 0);
        when(storeAccess.require(eq(actor), any(), anyString())).thenAnswer(call -> call.getArgument(1));
        when(storeAccess.requireOrAll(eq(actor), any(), anyString())).thenAnswer(call -> call.getArgument(1));
        when(storeAccess.allows(eq(actor), any(), anyString())).thenReturn(true);
    }

    @Test
    void requestShipReceiveMovesCounterStockWithLedgerEntries() {
        StockTransferDtos.Transfer created = asJob(() -> transfers.create(actor,
                new StockTransferDtos.CreateRequest(richStore, shortStore, "Cuối tuần bán chạy", List.of(new StockTransferDtos.CreateItem(tee, 8)))));
        assertThat(created.status()).isEqualTo("REQUESTED");
        assertThat(created.transferCode()).startsWith("CK-");
        assertThat(created.items()).singleElement().satisfies(item -> {
            assertThat(item.sourceStock()).isEqualTo(20);
            assertThat(item.size()).isEqualTo("M");
        });

        StockTransferDtos.Transfer shipped = asJob(() -> transfers.ship(actor, created.id(), Map.of(tee, 6)));
        assertThat(shipped.status()).isEqualTo("SHIPPED");
        assertThat(shipped.items().getFirst().shipped()).isEqualTo(6);
        assertThat(counter(richStore, tee)).isEqualTo(14);
        assertThat(counter(shortStore, tee)).isEqualTo(1);

        StockTransferDtos.Transfer received = asJob(() -> transfers.receive(actor, created.id()));
        assertThat(received.status()).isEqualTo("RECEIVED");
        assertThat(counter(shortStore, tee)).isEqualTo(7);
        assertThat(jdbc.queryForList("select transaction_type from inventory_transactions where reference_id = ? order by transaction_type",
                String.class, created.id())).containsExactly("TRANSFER_IN", "TRANSFER_OUT");
    }

    @Test
    void shippingIsLimitedByRequestAndSourceStock() {
        StockTransferDtos.Transfer created = asJob(() -> transfers.create(actor,
                new StockTransferDtos.CreateRequest(richStore, shortStore, null, List.of(new StockTransferDtos.CreateItem(teeL, 5)))));
        assertThatThrownBy(() -> asJob(() -> transfers.ship(actor, created.id(), Map.of(teeL, 6))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("số yêu cầu");
        assertThatThrownBy(() -> asJob(() -> transfers.ship(actor, created.id(), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không đủ để xuất");
        assertThatThrownBy(() -> asJob(() -> transfers.ship(actor, created.id(), Map.of(teeL, 0))))
                .hasMessageContaining("ít nhất một");
        assertThatThrownBy(() -> asJob(() -> transfers.receive(actor, created.id()))).hasMessageContaining("đang chuyển");
    }

    @Test
    void rejectAndCancelOnlyBeforeShipping() {
        assertThatThrownBy(() -> asJob(() -> transfers.create(actor,
                new StockTransferDtos.CreateRequest(shortStore, shortStore, null, List.of(new StockTransferDtos.CreateItem(tee, 1))))))
                .hasMessageContaining("khác cửa hàng nhận");

        StockTransferDtos.Transfer first = asJob(() -> transfers.create(actor,
                new StockTransferDtos.CreateRequest(richStore, shortStore, null, List.of(new StockTransferDtos.CreateItem(tee, 2)))));
        assertThat(asJob(() -> transfers.reject(actor, first.id(), "Đang cần cho chương trình")).rejectReason())
                .isEqualTo("Đang cần cho chương trình");

        StockTransferDtos.Transfer second = asJob(() -> transfers.create(actor,
                new StockTransferDtos.CreateRequest(richStore, shortStore, null, List.of(new StockTransferDtos.CreateItem(tee, 2)))));
        assertThat(asJob(() -> transfers.cancel(actor, second.id())).status()).isEqualTo("CANCELLED");
        assertThatThrownBy(() -> asJob(() -> transfers.ship(actor, second.id(), null))).hasMessageContaining("chờ duyệt");

        assertThat(transfers.list(actor, shortStore, "IN", null, Pageable.ofSize(10)).getContent())
                .extracting(StockTransferDtos.Transfer::status).containsExactly("CANCELLED", "REJECTED");
        assertThat(transfers.list(actor, shortStore, "OUT", null, Pageable.ofSize(10)).getContent()).isEmpty();
    }

    @Test
    void lowStockSuggestsTheStoreWithMostCounterStock() {
        var low = transfers.lowStock(actor, shortStore, "Zqtf", Pageable.ofSize(20)).getContent();
        assertThat(low).singleElement().satisfies(row -> {
            assertThat(row.variantId()).isEqualTo(tee);
            assertThat(row.total()).isEqualTo(2);
            assertThat(row.suggestedStore().id()).isEqualTo(richStore);
            assertThat(row.suggestedStock()).isEqualTo(20);
            assertThat(row.incoming()).isZero();
        });

        asJob(() -> transfers.create(actor,
                new StockTransferDtos.CreateRequest(richStore, shortStore, null, List.of(new StockTransferDtos.CreateItem(tee, 4)))));
        assertThat(transfers.lowStock(actor, shortStore, "Zqtf", Pageable.ofSize(20)).getContent().getFirst().incoming()).isEqualTo(4);
    }

    private <T> T asJob(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_TRANSFER")) {
            return action.get();
        }
    }

    private void variant(UUID id, String sku, String size) {
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Trắng', ?, 150000, true)",
                id, product, sku, size);
    }

    private void balance(UUID store, UUID variant, int counter, int online) {
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, ?, ?, 0)",
                store, variant, counter, online);
    }

    private int counter(UUID store, UUID variant) {
        return jdbc.queryForObject("select available_quantity from inventory_balances where store_id = ? and product_variant_id = ?",
                Integer.class, store, variant);
    }
}
