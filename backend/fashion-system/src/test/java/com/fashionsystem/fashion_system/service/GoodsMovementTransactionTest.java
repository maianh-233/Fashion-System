package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Opt-in against a disposable PostgreSQL server; each run owns a uniquely named schema. */
@SpringJUnitConfig(GoodsMovementTransactionTest.Config.class)
@EnabledIfSystemProperty(named = "warehouse.integration.url", matches = ".+")
class GoodsMovementTransactionTest {
    @Autowired GoodsReceiptService receipts;
    @Autowired GoodsIssueService issues;
    @Autowired GoodsReceiptRepository receiptRepository;
    @Autowired GoodsReceiptItemRepository receiptItems;
    @Autowired GoodsIssueRepository issueRepository;
    @Autowired GoodsIssueItemRepository issueItems;
    @Autowired InventoryService inventory;
    @Autowired SupplierRepository suppliers;
    @Autowired ProductVariantRepository variants;
    @Autowired PlatformTransactionManager transactions;
    @Autowired DataSource dataSource;
    UUID actor = UUID.randomUUID(), store = UUID.randomUUID(), supplier = UUID.randomUUID(), product = UUID.randomUUID();
    JdbcTemplate jdbc;

    @BeforeEach void setup() {
        reset(inventory, suppliers, variants);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table if not exists stock_probe (id uuid primary key, quantity integer not null)");
        when(suppliers.existsById(supplier)).thenReturn(true);
        when(variants.findById(any())).thenAnswer(call -> Optional.of(ProductVariant.builder()
                .id(call.getArgument(0)).productId(product).build()));
    }

    private UUID seed(boolean importing, int lines) {
        return new TransactionTemplate(transactions).execute(status -> {
            UUID documentId;
            if (importing) {
                documentId = receiptRepository.save(GoodsReceipt.builder().receiptCode("TEST-" + UUID.randomUUID())
                        .storeId(store).supplierId(supplier).status("CONFIRMED").receivedBy(actor)
                        .createdAt(LocalDateTime.now()).build()).getId();
            } else {
                documentId = issueRepository.save(GoodsIssue.builder().issueCode("TEST-" + UUID.randomUUID())
                        .storeId(store).supplierId(supplier).status("CONFIRMED").issuedBy(actor)
                        .issueType("DAMAGED").createdAt(LocalDateTime.now()).build()).getId();
            }
            for (int i = 0; i < lines; i++) {
                UUID variant = UUID.randomUUID();
                if (importing) receiptItems.save(GoodsReceiptItem.builder().receiptId(documentId)
                        .productId(product).productVariantId(variant).targetChannel("ONLINE").quantity(2)
                        .costPrice(BigDecimal.ONE).total(BigDecimal.valueOf(2)).createdAt(LocalDateTime.now()).build());
                else issueItems.save(GoodsIssueItem.builder().issueId(documentId).productId(product)
                        .productVariantId(variant).sourceChannel("ONLINE").quantity(2).createdAt(LocalDateTime.now()).build());
            }
            jdbc.update("insert into stock_probe(id, quantity) values (?, 0)", documentId);
            return documentId;
        });
    }

    private void stockAnswer(boolean importing, org.mockito.stubbing.Answer<?> answer) {
        if (importing) doAnswer(answer).when(inventory).receiveChannel(any(), any(), anyInt(), anyString(), any(), any());
        else doAnswer(answer).when(inventory).exportChannel(any(), any(), anyInt(), anyString(), anyString(), any(), any());
    }
    private void complete(boolean importing, UUID id) {
        if (importing) receipts.complete(actor, id); else issues.complete(actor, id);
    }
    private String status(boolean importing, UUID id) {
        return jdbc.queryForObject("select status from " + (importing ? "goods_receipts" : "goods_issues") + " where id = ?", String.class, id);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void secondLineFailureRollsBackStockAndDocument(boolean importing) {
        UUID id = seed(importing, 2);
        AtomicInteger calls = new AtomicInteger();
        stockAnswer(importing, call -> {
            jdbc.update("update stock_probe set quantity = quantity + 2 where id = ?", id);
            if (calls.incrementAndGet() == 2) throw BusinessException.invalidState("Second line cannot be fulfilled");
            return null;
        });
        assertThatThrownBy(() -> complete(importing, id)).isInstanceOf(BusinessException.class);
        assertThat(calls).hasValue(2);
        assertThat(jdbc.queryForObject("select quantity from stock_probe where id = ?", Integer.class, id)).isZero();
        assertThat(status(importing, id)).isEqualTo("CONFIRMED");
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void concurrentCompletionAppliesStockExactlyOnce(boolean importing) throws Exception {
        UUID id = seed(importing, 1);
        CountDownLatch insideFirst = new CountDownLatch(1), releaseFirst = new CountDownLatch(1);
        stockAnswer(importing, call -> {
            insideFirst.countDown();
            if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrency test timed out");
            jdbc.update("update stock_probe set quantity = quantity + 2 where id = ?", id);
            return null;
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> complete(importing, id));
            assertThat(insideFirst.await(10, TimeUnit.SECONDS)).isTrue();
            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<?> second = pool.submit(() -> { secondStarted.countDown(); complete(importing, id); });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            releaseFirst.countDown();
            first.get(15, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(15, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(BusinessException.class);
            assertThat(jdbc.queryForObject("select quantity from stock_probe where id = ?", Integer.class, id)).isEqualTo(2);
            assertThat(status(importing, id)).isEqualTo("COMPLETED");
        } finally { releaseFirst.countDown(); pool.shutdownNow(); }
    }

    @Configuration(proxyBeanMethods = false) @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            String url = System.getProperty("warehouse.integration.url");
            String user = System.getProperty("warehouse.integration.user", "warehouse_test");
            String password = System.getProperty("warehouse.integration.password", "");
            String schema = "receipt_test_" + UUID.randomUUID().toString().replace("-", "");
            var admin = new DriverManagerDataSource(url, user, password);
            new JdbcTemplate(admin).execute("create schema " + schema);
            return new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setManagedTypes(PersistenceManagedTypes.of(GoodsReceipt.class.getName(), GoodsReceiptItem.class.getName(),
                    GoodsIssue.class.getName(), GoodsIssueItem.class.getName()));
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean JpaRepositoryFactory repositories(EntityManagerFactory factory) {
            return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory));
        }
        @Bean GoodsReceiptRepository receiptRepository(JpaRepositoryFactory f) { return f.getRepository(GoodsReceiptRepository.class); }
        @Bean GoodsReceiptItemRepository receiptItems(JpaRepositoryFactory f) { return f.getRepository(GoodsReceiptItemRepository.class); }
        @Bean GoodsIssueRepository issueRepository(JpaRepositoryFactory f) { return f.getRepository(GoodsIssueRepository.class); }
        @Bean GoodsIssueItemRepository issueItems(JpaRepositoryFactory f) { return f.getRepository(GoodsIssueItemRepository.class); }
        @Bean SupplierRepository suppliers() { return mock(SupplierRepository.class); }
        @Bean ProductVariantRepository variants() { return mock(ProductVariantRepository.class); }
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean InventoryService inventory() { return mock(InventoryService.class); }
        @Bean StoreAccessService access() { return mock(StoreAccessService.class); }
        @Bean GoodsReceiptService receipts(GoodsReceiptRepository r, GoodsReceiptItemRepository i, SupplierRepository s,
                ProductVariantRepository v, ProductRepository p, InventoryService inventory, StoreAccessService access) {
            return new GoodsReceiptService(r, i, s, v, p, new GoodsReceiptMapper(), new GoodsReceiptItemMapper(), inventory, access);
        }
        @Bean GoodsIssueService issues(GoodsIssueRepository r, GoodsIssueItemRepository i, SupplierRepository s,
                ProductVariantRepository v, ProductRepository p, InventoryService inventory, StoreAccessService access) {
            return new GoodsIssueService(r, i, s, v, p, new GoodsIssueMapper(), new GoodsIssueItemMapper(), inventory, access);
        }
    }
}
