package com.fashionsystem.fashion_system.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.Catalog;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.CatalogGroup;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.CatalogModule;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.CatalogPermission;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.RoleDetails;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.RolePermissionView;
import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.dto.RoleDto;
import com.fashionsystem.fashion_system.dto.UserPermissionDto;
import com.fashionsystem.fashion_system.dto.UserRoleDto;
import com.fashionsystem.fashion_system.entity.PermissionEffect;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import java.time.LocalDateTime;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class RedisCacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withUserConfiguration(RedisCacheConfig.class);

    @Test
    void createsSharedCacheManagerOnlyWhenCacheIsEnabled() {
        contextRunner.withPropertyValues("redis.cache.enabled=true")
                .run(context -> {
                    assertThat(context.getBeansOfType(CacheManager.class)).hasSize(1);
                    RedisCacheManager manager = context.getBean(RedisCacheManager.class);
                    assertThat(manager.isAllowRuntimeCacheCreation()).isFalse();
                    assertThat(context).hasSingleBean(ResilientCacheErrorHandler.class);
                });

        contextRunner.withPropertyValues("redis.cache.enabled=false")
                .run(context -> assertThat(context.getBeansOfType(CacheManager.class)).isEmpty());
    }

    @Test
    void configuresOnlyRegisteredCachesWithCentralizedTtls() {
        RedisCacheProperties properties = new RedisCacheProperties();
        properties.setKeyPrefix("fs:test:v1");
        properties.setActiveModulesTtl(Duration.ofMinutes(11));
        properties.setAuthorizationCatalogTtl(Duration.ofMinutes(6));
        properties.setAuthorizationRoleTtl(Duration.ofMinutes(3));
        properties.setAuthorizationEffectivePermissionTtl(Duration.ofSeconds(20));
        properties.setAuthorizationUserAssignmentTtl(Duration.ofSeconds(45));
        properties.setCatalogTtl(Duration.ofMinutes(12));
        properties.setProductTtl(Duration.ofMinutes(4));
        properties.setPromotionTtl(Duration.ofSeconds(40));

        RedisCacheConfig config = new RedisCacheConfig(
                properties, mock(RedisConnectionFactory.class));
        var cacheConfigurations = config.cacheConfigurations();

        assertThat(cacheConfigurations)
                .containsOnlyKeys(
                        CacheNames.ACTIVE_MODULES,
                        CacheNames.AUTHORIZATION_MODULE_LIST,
                        CacheNames.AUTHORIZATION_MODULE_DETAIL,
                        CacheNames.AUTHORIZATION_GROUP_LIST,
                        CacheNames.AUTHORIZATION_GROUP_DETAIL,
                        CacheNames.AUTHORIZATION_PERMISSION_LIST,
                        CacheNames.AUTHORIZATION_PERMISSION_DETAIL,
                        CacheNames.AUTHORIZATION_CATALOG,
                        CacheNames.AUTHORIZATION_ROLE_LIST,
                        CacheNames.AUTHORIZATION_ROLE_DETAIL,
                        CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS,
                        CacheNames.AUTHORIZATION_USER_ROLE_LIST,
                        CacheNames.AUTHORIZATION_USER_PERMISSION_LIST,
                        CacheNames.BRAND_DETAIL,
                        CacheNames.CATEGORY_DETAIL,
                        CacheNames.COLLECTION_DETAIL,
                        CacheNames.PRODUCT_DETAIL,
                        CacheNames.PRODUCT_VARIANT_DETAIL,
                        CacheNames.PRODUCT_ATTRIBUTE_DETAIL,
                        CacheNames.PRODUCT_TAG_DETAIL,
                        CacheNames.PROMOTION_DETAIL,
                        CacheNames.STORE_DETAIL,
                        CacheNames.DEPARTMENT_DETAIL,
                        CacheNames.POSITION_DETAIL,
                        CacheNames.SUPPLIER_DETAIL,
                        CacheNames.CUSTOMER_TIER_DETAIL,
                        CacheNames.CUSTOMER_TIER_ORDERED_LIST);
        assertThat(cacheConfigurations.get(CacheNames.ACTIVE_MODULES)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofMinutes(11));
        assertThat(cacheConfigurations.get(CacheNames.AUTHORIZATION_CATALOG)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofMinutes(6));
        assertThat(cacheConfigurations.get(CacheNames.AUTHORIZATION_ROLE_DETAIL)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofMinutes(3));
        assertThat(cacheConfigurations.get(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofSeconds(20));
        assertThat(cacheConfigurations.get(CacheNames.AUTHORIZATION_USER_ROLE_LIST)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofSeconds(45));
        assertThat(cacheConfigurations.get(CacheNames.BRAND_DETAIL)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofMinutes(12));
        assertThat(cacheConfigurations).containsKey(CacheNames.POSITION_DETAIL);
        assertThat(cacheConfigurations.get(CacheNames.POSITION_DETAIL)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofMinutes(12));
        assertThat(cacheConfigurations.get(CacheNames.PRODUCT_DETAIL)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofMinutes(4));
        assertThat(cacheConfigurations.get(CacheNames.PROMOTION_DETAIL)
                .getTtlFunction().getTimeToLive("cache", "key".getBytes())).isEqualTo(Duration.ofSeconds(40));
        assertThat(cacheConfigurations.get(CacheNames.ACTIVE_MODULES)
                .getKeyPrefixFor(CacheNames.ACTIVE_MODULES))
                .isEqualTo("fs:test:v1:" + CacheNames.ACTIVE_MODULES + "::");
    }

    @Test
    void jsonSerializerRoundTripsCachedDtoLists() {
        RedisCacheProperties properties = new RedisCacheProperties();
        RedisCacheConfig config = new RedisCacheConfig(
                properties, mock(RedisConnectionFactory.class));
        var serialization = config.cacheConfigurations().get(CacheNames.ACTIVE_MODULES)
                .getValueSerializationPair();
        List<ModuleDto> expected = new ArrayList<>(List.of(ModuleDto.builder()
                .id(UUID.randomUUID())
                .code("CATALOG")
                .name("Catalog")
                .active(true)
                .build()));

        Object actual = serialization.read(serialization.write(expected));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void jsonSerializerRoundTripsNestedCatalogAndRoleDetails() {
        RedisCacheConfig config = new RedisCacheConfig(
                new RedisCacheProperties(), mock(RedisConnectionFactory.class));
        var catalogSerialization = config.cacheConfigurations().get(CacheNames.AUTHORIZATION_CATALOG)
                .getValueSerializationPair();
        var roleSerialization = config.cacheConfigurations().get(CacheNames.AUTHORIZATION_ROLE_DETAIL)
                .getValueSerializationPair();
        UUID permissionId = UUID.randomUUID();
        Catalog catalog = new Catalog(new ArrayList<>(List.of(new CatalogModule(
                UUID.randomUUID(), "CATALOG", "Catalog", "box", 1, true,
                new ArrayList<>(List.of(new CatalogGroup(
                        UUID.randomUUID(), "PRODUCT", "Product",
                        new ArrayList<>(List.of(new CatalogPermission(
                                permissionId, "PRODUCT_READ", "Read product"))))))))));
        RoleDetails roleDetails = new RoleDetails(
                RoleDto.builder().id(UUID.randomUUID()).code("ADMIN").name("Admin").build(),
                new ArrayList<>(List.of(new RolePermissionView(
                        permissionId, "PRODUCT_READ", "Read product",
                        PermissionScope.ALL, "PRODUCT", "CATALOG"))));

        assertThat(catalogSerialization.read(catalogSerialization.write(catalog))).isEqualTo(catalog);
        assertThat(roleSerialization.read(roleSerialization.write(roleDetails))).isEqualTo(roleDetails);
    }

    @Test
    void jsonSerializerRoundTripsEffectivePermissionList() {
        RedisCacheConfig config = new RedisCacheConfig(
                new RedisCacheProperties(), mock(RedisConnectionFactory.class));
        var serialization = config.cacheConfigurations()
                .get(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS)
                .getValueSerializationPair();
        List<EffectivePermissionDto> expected = new ArrayList<>(List.of(new EffectivePermissionDto(
                UUID.randomUUID(), "PRODUCT_READ", "Read product", "PRODUCT", "Product",
                "CATALOG", "Catalog", null, "box", 1, PermissionScope.ALL, "ROLE")));

        assertThat(serialization.read(serialization.write(expected))).isEqualTo(expected);
    }

    @Test
    void jsonSerializerRoundTripsUserAuthorizationLists() {
        RedisCacheConfig config = new RedisCacheConfig(
                new RedisCacheProperties(), mock(RedisConnectionFactory.class));
        UUID userId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        List<UserRoleDto> roles = new ArrayList<>(List.of(UserRoleDto.builder()
                .userId(userId).roleId(assignmentId).assignedAt(LocalDateTime.of(2026, 8, 28, 15, 0)).build()));
        List<UserPermissionDto> permissions = new ArrayList<>(List.of(UserPermissionDto.builder()
                .userId(userId).permissionId(assignmentId)
                .effect(PermissionEffect.DENY).scope(PermissionScope.ALL).build()));
        var roleSerialization = config.cacheConfigurations().get(CacheNames.AUTHORIZATION_USER_ROLE_LIST)
                .getValueSerializationPair();
        var permissionSerialization = config.cacheConfigurations()
                .get(CacheNames.AUTHORIZATION_USER_PERMISSION_LIST).getValueSerializationPair();

        assertThat(roleSerialization.read(roleSerialization.write(roles))).isEqualTo(roles);
        assertThat(permissionSerialization.read(permissionSerialization.write(permissions)))
                .isEqualTo(permissions);
    }

    @Test
    void jsonSerializerRoundTripsBusinessCatalogDtos() {
        RedisCacheConfig config = new RedisCacheConfig(
                new RedisCacheProperties(), mock(RedisConnectionFactory.class));
        ProductDto product = ProductDto.builder()
                .id(UUID.randomUUID()).name("Shirt").imageUrl("shirt.jpg").build();
        PromotionDto promotion = PromotionDto.builder()
                .id(UUID.randomUUID()).code("SALE").name("Sale")
                .discountType("PERCENT").discountValue(BigDecimal.TEN).build();
        var productSerialization = config.cacheConfigurations().get(CacheNames.PRODUCT_DETAIL)
                .getValueSerializationPair();
        var promotionSerialization = config.cacheConfigurations().get(CacheNames.PROMOTION_DETAIL)
                .getValueSerializationPair();

        assertThat(productSerialization.read(productSerialization.write(product))).isEqualTo(product);
        assertThat(promotionSerialization.read(promotionSerialization.write(promotion))).isEqualTo(promotion);
    }

}
