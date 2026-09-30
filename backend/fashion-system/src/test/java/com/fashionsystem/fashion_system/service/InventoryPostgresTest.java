package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.mapper.*;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises production SQL upsert, JPA row locking and atomic ledger writes on disposable PostgreSQL. */
@SpringJUnitConfig(InventoryPostgresTest.Config.class)
@EnabledIfSystemProperty(named="warehouse.integration.url",matches=".+")
class InventoryPostgresTest {
    @Autowired InventoryService service;
    @Autowired DataSource ds;
    @Autowired PlatformTransactionManager tm;
    @Autowired InventoryBalanceRepository balances;
    final UUID actor=UUID.randomUUID(),store=UUID.randomUUID(),variant=UUID.randomUUID();
    JdbcTemplate jdbc;
    @BeforeEach void setup() { jdbc=new JdbcTemplate(ds); }

    @Test void realConcurrentExportsCannotOversell() throws Exception {
        audited(()->service.receiveChannel(store,variant,10,"ONLINE",UUID.randomUUID(),actor));
        var pool=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        try {
            var first=pool.submit(()->export(start,8)); var second=pool.submit(()->export(start,5));
            start.countDown();
            assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(stock()).isIn(2,5);
            assertThat(countMovements()).isEqualTo(2);
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentFirstImportsCreateOneBalance() throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            Callable<Void> action=()->{start.await();audited(()->service.receiveChannel(store,variant,1,"ONLINE",UUID.randomUUID(),actor));return null;};
            var a=pool.submit(action);var b=pool.submit(action);start.countDown();
            a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
            assertThat(stock()).isEqualTo(2);
            assertThat(countMovements()).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from inventory_balances where store_id=? and product_variant_id=?",Long.class,store,variant)).isEqualTo(1L);
        } finally {pool.shutdownNow();}
    }
    @Test void failureRollsBackEarlierStockAndLedgerInTransaction() {
        audited(()->service.receiveChannel(store,variant,5,"ONLINE",UUID.randomUUID(),actor));
        assertThatThrownBy(()->new TransactionTemplate(tm).executeWithoutResult(status->{
            audited(()->service.exportChannel(store,variant,3,"ONLINE","DAMAGED",UUID.randomUUID(),actor));
            audited(()->service.exportChannel(store,variant,3,"ONLINE","DAMAGED",UUID.randomUUID(),actor));
        })).isInstanceOf(com.fashionsystem.fashion_system.exception.BusinessException.class);
        assertThat(stock()).isEqualTo(5);assertThat(countMovements()).isEqualTo(1);
    }
    @Test void transferSnapshotsAndQueryAggregatesUseBothChannels() {
        audited(()->service.receiveChannel(store,variant,10,"ONLINE",UUID.randomUUID(),actor));
        audited(()->service.exportChannel(store,variant,3,"ONLINE","ONLINE_TO_OFFLINE",UUID.randomUUID(),actor));
        assertThat(stock()).isEqualTo(7);
        var last=jdbc.queryForMap("select * from inventory_transactions where store_id=? and transaction_type='ONLINE_TO_OFFLINE'",store);
        assertThat(last).containsEntry("before_online",10).containsEntry("after_online",7).containsEntry("before_offline",0).containsEntry("after_offline",3).containsEntry("balance_after",10);
        // Both JPQL query paths are parsed against real mapped types, including projections.
        new TransactionTemplate(tm).executeWithoutResult(status->{
            assertThat(balances.searchWarehouse(store,null,null,"",null,"ALL",PageRequest.of(0,20))).isNotNull();
            assertThat(balances.summarize(store,5)).isNotNull();
        });
    }
    private boolean export(CountDownLatch start,int quantity) throws Exception {
        start.await();
        try {audited(()->service.exportChannel(store,variant,quantity,"ONLINE","DAMAGED",UUID.randomUUID(),actor));return true;}
        catch(com.fashionsystem.fashion_system.exception.BusinessException expected) {return false;}
    }
    private void audited(Runnable action) {
        new TransactionTemplate(tm).executeWithoutResult(status -> {
            try (var audit = com.fashionsystem.fashion_system.audit.AuditCaptureScope.open(new com.fashionsystem.fashion_system.audit.AuditChangeCollector())) { action.run(); }
        });
    }
    private int stock(){return jdbc.queryForObject("select online_quantity from inventory_balances where store_id=? and product_variant_id=?",Integer.class,store,variant);}
    private int countMovements(){return jdbc.queryForObject("select count(*) from inventory_transactions where store_id=? and product_variant_id=?",Integer.class,store,variant);}

    @Configuration(proxyBeanMethods=false) @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() {
            String url=System.getProperty("warehouse.integration.url"),user=System.getProperty("warehouse.integration.user","warehouse_test"),password=System.getProperty("warehouse.integration.password","");
            String schema="inventory_test_"+UUID.randomUUID().toString().replace("-","");
            new JdbcTemplate(new DriverManagerDataSource(url,user,password)).execute("create schema "+schema);
            return new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var f=new LocalContainerEntityManagerFactoryBean();f.setDataSource(ds);f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            f.setManagedTypes(PersistenceManagedTypes.of(InventoryBalance.class.getName(),InventoryTransaction.class.getName(),Store.class.getName(),Product.class.getName(),ProductVariant.class.getName()));
            f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","create-drop","hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));return f;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory f){return new JpaTransactionManager(f);}
        @Bean JpaRepositoryFactory repositories(EntityManagerFactory f){return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f));}
        @Bean InventoryBalanceRepository balances(JpaRepositoryFactory f){return f.getRepository(InventoryBalanceRepository.class);}
        @Bean InventoryTransactionRepository movements(JpaRepositoryFactory f){return f.getRepository(InventoryTransactionRepository.class);}
        @Bean StoreRepository stores(){var r=mock(StoreRepository.class);when(r.existsById(any())).thenReturn(true);return r;}
        @Bean ProductVariantRepository variants(){var r=mock(ProductVariantRepository.class);when(r.existsById(any())).thenReturn(true);return r;}
        @Bean ProductRepository products(){return mock(ProductRepository.class);}
        @Bean StoreAccessService access(){var r=mock(StoreAccessService.class);when(r.require(any(),any(),anyString())).thenAnswer(i->i.getArgument(1));return r;}
        @Bean UserScopeService scopes(){return mock(UserScopeService.class);}
        @Bean InventoryService inventory(InventoryBalanceRepository b,InventoryTransactionRepository t,StoreRepository s,ProductVariantRepository v,ProductRepository p,StoreAccessService a,UserScopeService u){return new InventoryService(b,t,s,v,p,new InventoryBalanceMapper(),new InventoryTransactionMapper(),a,u);}
    }
}
