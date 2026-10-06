# Backend Read Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bổ sung Redis cache an toàn cho các API list/detail catalog và master data phù hợp, với cache key đầy đủ và invalidation chính xác ở mọi mutation liên quan.

**Architecture:** Giữ `RedisCacheManager` và Spring Cache hiện có. Các service nghiệp vụ kiểm tra/chuẩn hóa đầu vào rồi gọi một `CatalogReadCacheService` riêng để cache `CachedPage<T>`; cách này tránh self-invocation và tránh phụ thuộc khả năng deserialize `PageImpl`. Warehouse lookup luôn kiểm tra quyền trước khi gọi lớp cache, nên cache hit không thể bỏ qua authorization.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Cache, Spring Data Redis, Spring Data JPA, JUnit 5, Mockito, AssertJ.

**Spec:** `docs/superpowers/specs/2026-10-06-backend-read-cache-design.md`

## Global Constraints

- Không dùng multi-agent; triển khai tuần tự trong phiên hiện tại.
- Không cache mutation, inventory, goods receipt/issue, audit log, customer/employee/profile hoặc workflow real-time.
- Không thay đổi contract HTTP hiện tại.
- Mọi list key phải chứa đầy đủ filter, page, size và toàn bộ sort; thêm store/scope khi kết quả phụ thuộc chúng.
- Cache hit không được bỏ qua authorization.
- Chỉ dùng Spring Cache và Redis config hiện có; không thêm dependency hoặc cache backend mới.
- Mọi behavior mới hoặc bug fix phải theo chu trình test đỏ → code tối thiểu → test xanh.

## Review Focus

- Hai bộ filter chứa delimiter hoặc biểu diễn chuỗi gần giống nhau vẫn phải tạo key khác nhau — kiểm tra tại Task 1.
- Hai `Pageable` khác size, sort direction, ignore-case hoặc null handling phải tạo key khác nhau — kiểm tra tại Task 1.
- `Page<DTO>` cache phải round-trip qua Redis serializer mà không cần whitelist rộng — kiểm tra tại Task 1.
- Cache warehouse không được trả dữ liệu trước khi kiểm tra quyền/store — kiểm tra tại Task 5.
- Mutation làm thay đổi dữ liệu nhúng từ aggregate khác phải xóa các cache phụ thuộc — kiểm tra tại Task 3, 4 và 5.

---

### Task 1: Cache key và page envelope an toàn

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/CacheKeys.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/CachedPage.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/CacheNames.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/RedisCacheConfig.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/config/CacheKeysTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/config/RedisCacheConfigTest.java`

**Interfaces:**
- Produces: `CacheKeys.of(Object... parts): String`.
- Produces: `CachedPage.from(Page<T>): CachedPage<T>` and `CachedPage.toPage(): Page<T>` preserving content, page number, page size, total elements and sort.
- Produces: registered list/image/warehouse cache constants with catalog/product/promotion TTLs.

- [ ] **Step 1: Write failing key tests**

Add tests asserting `CacheKeys.of(...)` differs for null versus empty, embedded delimiters, page number, size, sort direction, ignore-case and null handling; identical semantic inputs produce identical keys.

- [ ] **Step 2: Run key tests and verify RED**

Run: `mvn -q -Dtest=CacheKeysTest test`
Expected: FAIL because `CacheKeys` does not exist.

- [ ] **Step 3: Implement canonical cache keys**

Implement length-prefixed, type-aware encoding for scalar values, enum/UUID and `Pageable`/`Sort.Order`. Reject unsupported mutable input types instead of falling back to ambiguous `toString()`.

- [ ] **Step 4: Run key tests and verify GREEN**

Run: `mvn -q -Dtest=CacheKeysTest test`
Expected: PASS.

- [ ] **Step 5: Write failing cached-page serializer test**

Extend `RedisCacheConfigTest` with a `CachedPage<ProductDto>` containing multiple sort orders and assert Redis serialization/deserialization preserves all page metadata and DTO content. Assert all new cache names are registered with the expected TTL group.

- [ ] **Step 6: Run serializer/config tests and verify RED**

Run: `mvn -q -Dtest=RedisCacheConfigTest test`
Expected: FAIL because `CachedPage` and new cache names/configurations do not exist.

- [ ] **Step 7: Implement page envelope and cache registration**

Create the DTO envelope using only allowed DTO/JDK value types, add narrowly scoped cache names, and register them in `RedisCacheConfig` without broadening the polymorphic type whitelist.

- [ ] **Step 8: Run Task 1 tests**

Run: `mvn -q -Dtest=CacheKeysTest,RedisCacheConfigTest test`
Expected: PASS.

- [ ] **Step 9: Commit Task 1**

Commit only the cache key, cached-page, registry/config and their tests with message `feat: add safe Redis list cache primitives`.

### Task 2: Spring Cache facade cho list reads

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CatalogReadCacheService.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CatalogReadCacheServiceTest.java`

**Interfaces:**
- Consumes: canonical String key and a `Supplier<Page<T>>`/`Supplier<List<T>>` loader that is invoked only on cache miss.
- Produces: type-specific methods for brand, category, collection, product, variant, attribute, tag, promotion, store, department, position, supplier, customer-tier, product-image and warehouse lookup caches.

- [ ] **Step 1: Write failing cache-hit and key-isolation tests**

Assert repeated reads with the same key invoke the loader once, different keys invoke separate loaders, and cached mutable lists are returned as detached list values suitable for Redis serialization.

- [ ] **Step 2: Run facade test and verify RED**

Run: `mvn -q -Dtest=CatalogReadCacheServiceTest test`
Expected: FAIL because the facade does not exist.

- [ ] **Step 3: Implement the minimal annotated facade**

Each type-specific public method uses a static cache name and `key = "#key"`; page methods cache `CachedPage<T>` then callers convert it back to `Page<T>`. Do not place authorization inside this facade.

- [ ] **Step 4: Run facade test and verify GREEN**

Run: `mvn -q -Dtest=CatalogReadCacheServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit Task 2**

Commit the facade and its tests with message `feat: add catalog read cache facade`.

### Task 3: Catalog/product list cache và invalidation

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/BrandService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CategoryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CollectionService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductVariantService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductAttributeService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductTagService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductImageService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductTagMappingService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CatalogMediaService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CatalogListCacheIntegrationTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/BusinessCatalogReadCacheServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CatalogLifecycleServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ProductVariantServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ProductImageServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/PromotionServiceTest.java`

**Interfaces:**
- Consumes: `CatalogReadCacheService` and `CacheKeys.of`.
- Produces: unchanged service return types and HTTP-visible behavior.

- [ ] **Step 1: Write failing catalog list cache tests**

For representative brand and product queries, assert same normalized filters/page/sort hit the repository once and changed filter/page/sort performs another query. Add image list/detail cache assertions scoped by product, variant and image IDs.

- [ ] **Step 2: Write failing mutation invalidation tests**

Assert create/update/delete/restore clears its aggregate list cache; brand/category/collection state changes clear product and variant list/detail caches; product/variant/tag mapping/media/image mutations clear every dependent list/detail cache that can contain changed fields.

- [ ] **Step 3: Run catalog cache tests and verify RED**

Run: `mvn -q -Dtest=CatalogListCacheIntegrationTest,BusinessCatalogReadCacheServiceTest test`
Expected: FAIL because list/image caches and invalidation are missing.

- [ ] **Step 4: Wire catalog reads through the cache facade**

Validate sort/ranges before the facade call, build keys from every effective filter plus `Pageable`, and keep DTO mapping inside the loader. Do not include raw authentication objects in keys.

- [ ] **Step 5: Add catalog mutation invalidation**

Use `@Caching` with targeted detail keys and `allEntries = true` for list/dependent caches whose full key set cannot be enumerated. Preserve existing transaction and audit annotations.

- [ ] **Step 6: Run catalog tests and verify GREEN**

Run: `mvn -q -Dtest=CatalogListCacheIntegrationTest,BusinessCatalogReadCacheServiceTest test`
Expected: PASS.

- [ ] **Step 7: Update direct-construction tests and commit Task 3**

Supply the cache facade mock/test bean to every existing test that constructs an affected service directly, rerun those classes, then commit catalog service/test changes with message `feat: cache catalog read lists`.

### Task 4: Reference/master-data list cache và invalidation

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StoreService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/DepartmentService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PositionService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/SupplierService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerTierService.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ReferenceListCacheIntegrationTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/DepartmentReadCacheServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/StoreServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/DepartmentServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/PositionServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/SupplierServiceTest.java`

**Interfaces:**
- Consumes: `CatalogReadCacheService` and canonical keys.
- Produces: cached pageable master-data reads with unchanged public signatures.

- [ ] **Step 1: Write failing reference cache tests**

Assert store/department/position/supplier/customer-tier repeated list reads hit repositories once and filters/page/sort isolate entries.

- [ ] **Step 2: Write failing reference invalidation tests**

Assert every create/update/delete/restore clears its list cache. Department mutation must additionally clear position detail/list because position DTO embeds department data; tier mutation must clear both pageable and ordered-tier caches.

- [ ] **Step 3: Run reference tests and verify RED**

Run: `mvn -q -Dtest=ReferenceListCacheIntegrationTest,DepartmentReadCacheServiceTest test`
Expected: FAIL because reference list caches/invalidation are missing.

- [ ] **Step 4: Implement reference caching and invalidation**

Keep validation ahead of cache lookup, use all filters plus complete `Pageable` in keys, and preserve existing detail `@CachePut`/`@CacheEvict` behavior.

- [ ] **Step 5: Run reference tests and verify GREEN**

Run: `mvn -q -Dtest=ReferenceListCacheIntegrationTest,DepartmentReadCacheServiceTest test`
Expected: PASS.

- [ ] **Step 6: Update direct-construction tests and commit Task 4**

Supply the cache facade mock/test bean to existing reference-service tests, rerun those classes, then commit with message `feat: cache reference data lists`.

### Task 5: Warehouse lookup cache sau authorization

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/WarehouseLookupService.java`
- Modify: catalog/reference mutation services from Tasks 3–4 only where warehouse invalidation is required.
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/WarehouseLookupCacheServiceTest.java`

**Interfaces:**
- Consumes: warehouse-specific methods of `CatalogReadCacheService`.
- Produces: cached product/supplier/category/variant lookup data keyed by resolved store, filters and page metadata.

- [ ] **Step 1: Write failing authorization-order tests**

Warm a warehouse lookup cache, deny the next access check, and assert the second call throws forbidden rather than returning cached data. Assert different stores, filters and pageable values cannot share entries.

- [ ] **Step 2: Write failing warehouse invalidation tests**

Assert product/variant/category/supplier mutations clear their corresponding warehouse lookup caches.

- [ ] **Step 3: Run warehouse tests and verify RED**

Run: `mvn -q -Dtest=WarehouseLookupCacheServiceTest test`
Expected: FAIL because warehouse lookup caching/invalidation is absent.

- [ ] **Step 4: Implement post-authorization warehouse caching**

Call `StoreAccessService.require/requireAny` on every request before invoking the cache facade. Key on the resolved store ID, normalized keyword/product ID and effective page/sort; do not cache `accessibleStores` or authorization results here.

- [ ] **Step 5: Add cross-module warehouse invalidation and run tests**

Run: `mvn -q -Dtest=WarehouseLookupCacheServiceTest,CatalogListCacheIntegrationTest,ReferenceListCacheIntegrationTest test`
Expected: PASS.

- [ ] **Step 6: Commit Task 5**

Commit warehouse lookup cache and related invalidation tests with message `feat: cache authorized warehouse lookups`.

### Task 6: Toàn bộ backend audit và verification

**Files:**
- Review: all files under `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller`
- Review: all files under `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service`
- Review: all files under `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository`
- Modify/Test: only cache-related files when the audit finds a concrete defect.

**Interfaces:**
- Consumes: final cache registry, service annotations and endpoint inventory.
- Produces: final API/cache/invalidation inventory for the handoff report.

- [ ] **Step 1: Run static cache audit**

Enumerate every GET endpoint, its service/repository path, cache name/key and mutation invalidators. Verify deliberately uncached APIs match the spec and no mutation carries `@Cacheable`.

- [ ] **Step 2: Add a failing regression test for each concrete defect found**

For any missing invalidation, collision or authorization bypass found during the audit, first add the smallest test reproducing it and confirm RED before changing production code.

- [ ] **Step 3: Apply minimal fixes and rerun focused tests**

Run the owning test class for each fix until GREEN; do not refactor unrelated modules.

- [ ] **Step 4: Run all cache-focused tests**

Run: `mvn -q -Dtest='*Cache*Test,Redis*Test' test`
Expected: PASS with zero failures/errors.

- [ ] **Step 5: Run the full backend suite**

Run: `mvn test -q`
Expected: PASS with zero failures/errors. If a pre-existing failure remains, record its exact test and error without hiding it.

- [ ] **Step 6: Run clean compile**

Run: `mvn -q -DskipTests clean compile`
Expected: BUILD SUCCESS, exit code 0.

- [ ] **Step 7: Review final diff**

Run `git diff --check`, inspect scoped status/diff, confirm no frontend or unrelated module changed, and produce the requested four-item Vietnamese summary.

- [ ] **Step 8: Commit verified audit fixes**

If Task 6 introduced additional regression-tested cache fixes, commit only those files with message `fix: close backend cache invalidation gaps`; otherwise do not create an empty commit.
