# Product and Inventory Store Scope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce Global read/write Product master data and Store-owned Inventory authorization end to end, connect the admin UI to scoped APIs, and persist validated Cloudinary image identifiers.

**Architecture:** A shared `UserScopeService` resolves identity scope from active `StoreStaff` assignments, independently from RBAC grants. Product services require effective permission plus Global identity for mutations; Inventory and receipt/issue services require effective permission and derive or validate Store ownership before database access. Existing PostgreSQL transactions/row locks and resilient Product caches remain authoritative; Cloudinary is isolated behind a reusable storage adapter.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, PostgreSQL, JUnit 5/Mockito/MockMvc, Cloudinary Java SDK, React 19, Vite, ESLint.

**Spec:** `docs/superpowers/specs/2026-09-12-product-inventory-store-scope-design.md`

## Global Constraints

- Product master data has no Store ownership; Store users may read but never mutate it.
- Every operation requires effective RBAC permission; scope only narrows access and never grants an action.
- A Store user is derived from active `StoreStaff`, never a hard-coded role name or request parameter.
- Inventory list, count, detail, mutation, transaction history, receipts, issues, and statistics are database-scoped.
- Backend is the authorization source of truth; frontend controls are presentation only.
- Persist only `secure_url` and `public_id` returned by Cloudinary after server-side file validation.
- Keep Redis as optional resilient Product cache; do not add Kafka or Redis locks.
- Preserve unrelated dirty-worktree changes and existing UI styling.

---

### Task 1: Shared User Identity Scope

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/UserScope.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/UserScopeService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/UserScopeResponse.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/UserScopeController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeDataScopeService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/UserScopeServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/UserScopeControllerSecurityTest.java`

**Interfaces:**
- Produces: `UserScope resolve(UUID userId)`, `void requireGlobal(UUID userId)`, `UUID resolveStoreId(UUID userId, UUID requestedStoreId)`, `void requireStoreAccess(UUID userId, UUID persistedStoreId)`.
- Produces: `GET /api/me/scope -> UserScopeResponse(scope, storeId, storeCode, storeName)`.
- Consumes: `StoreStaffRepository.findAllByUserIdAndActiveTrue`, `StoreRepository.findById`.

- [ ] **Step 1: Write failing scope resolution tests**

```java
@Test
void userWithOneActiveStoreIsStoreScopedEvenWhenRbacCouldBeAll() {
    when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId))
            .thenReturn(List.of(StoreStaff.builder().userId(userId).storeId(storeId).active(true).build()));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore(storeId)));
    assertThat(service.resolve(userId).kind()).isEqualTo(UserScope.Kind.STORE);
}

@Test
void storeUserCannotRequestAnotherStore() {
    stubStoreUser();
    assertThatThrownBy(() -> service.resolveStoreId(userId, otherStoreId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./mvnw -q -Dtest=UserScopeServiceTest,UserScopeControllerSecurityTest test`

Expected: compilation failure because `UserScopeService` and `/api/me/scope` do not exist.

- [ ] **Step 3: Implement the minimal scope resolver and endpoint**

```java
public record UserScope(Kind kind, UUID storeId, String storeCode, String storeName) {
    public enum Kind { GLOBAL, STORE }
    public boolean isGlobal() { return kind == Kind.GLOBAL; }
}

@Transactional(readOnly = true)
public UUID resolveStoreId(UUID userId, UUID requestedStoreId) {
    UserScope scope = resolve(userId);
    if (scope.isGlobal()) return requestedStoreId;
    if (requestedStoreId != null && !requestedStoreId.equals(scope.storeId())) {
        throw BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này");
    }
    return scope.storeId();
}
```

`resolve` treats zero distinct active assignments as Global, one as Store after validating the Store is active, and more than one as forbidden. `EmployeeDataScopeService` delegates Store identity checks to this service while retaining its permission-specific API.

- [ ] **Step 4: Run scope tests and the existing employee-scope suite**

Run: `./mvnw -q -Dtest=UserScopeServiceTest,UserScopeControllerSecurityTest,EmployeeDataScopeServiceTest,EmployeeAdministrationServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit the isolated scope foundation**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/UserScope.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/UserScopeService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/UserScopeResponse.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/UserScopeController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeDataScopeService.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/UserScopeServiceTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/UserScopeControllerSecurityTest.java
git commit -m "feat(auth): resolve global and store user scope"
```

### Task 2: Product Master Authorization Boundary

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductAuthorizationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/BrandController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CategoryController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CollectionController.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ProductAuthorizationServiceTest.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductMasterControllerSecurityTest.java`

**Interfaces:**
- Consumes: `AuthorizationService.hasPermission(UUID, String)` and `UserScopeService.requireGlobal(UUID)`.
- Produces: `void requireRead(UUID actorId, String permissionCode)` and `void requireMutation(UUID actorId, String permissionCode)`.

- [ ] **Step 1: Write failing tests for RBAC AND Global scope**

```java
@Test
void storeUserWithDatabaseMutationPermissionIsStillDenied() {
    when(authorizationService.hasPermission(actorId, "BRAND_CREATE")).thenReturn(true);
    doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
            .when(userScopeService).requireGlobal(actorId);
    assertThatThrownBy(() -> service.requireMutation(actorId, "BRAND_CREATE"))
            .isInstanceOf(BusinessException.class);
}

@Test
void readNeedsPermissionButDoesNotRequireGlobalScope() {
    when(authorizationService.hasPermission(actorId, "PRODUCT_VIEW")).thenReturn(true);
    service.requireRead(actorId, "PRODUCT_VIEW");
    verify(userScopeService, never()).requireGlobal(actorId);
}
```

Controller tests authenticate an `AuthenticatedUser` and assert Store Product mutation is `403`, missing permission is `403`, and permitted read is `200`.

- [ ] **Step 2: Run tests and verify RED**

Run: `./mvnw -q -Dtest=ProductAuthorizationServiceTest,ProductMasterControllerSecurityTest test`

Expected: FAIL because Product authorization is currently authentication-only.

- [ ] **Step 3: Implement centralized Product authorization**

```java
public void requireMutation(UUID actorId, String permissionCode) {
    requirePermission(actorId, permissionCode);
    userScopeService.requireGlobal(actorId);
}

private void requirePermission(UUID actorId, String permissionCode) {
    if (!authorizationService.hasPermission(actorId, permissionCode)) {
        throw BusinessException.forbidden("Bạn không có quyền thực hiện thao tác này");
    }
}
```

Brand/Category/Collection controllers pass the principal actor id into authorization immediately before invoking the existing service. Read endpoints use `*_VIEW`; mutations use `*_CREATE`, `*_UPDATE`, or `*_DELETE`.

- [ ] **Step 4: Run focused and existing catalog/cache tests**

Run: `./mvnw -q -Dtest=ProductAuthorizationServiceTest,ProductMasterControllerSecurityTest,BusinessCatalogReadCacheServiceTest,RedisFailureFallbackTest test`

Expected: PASS and no regression in cache behavior.

- [ ] **Step 5: Commit Product master authorization**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductAuthorizationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/BrandController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CategoryController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/CollectionController.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ProductAuthorizationServiceTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductMasterControllerSecurityTest.java
git commit -m "feat(product): require global scope for catalog mutations"
```

### Task 3: Complete Product, Variant, Tag, and Attribute APIs

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductVariantController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductTagController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductAttributeController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/SecurityConfig.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductControllerSecurityTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductVariantControllerSecurityTest.java`

**Interfaces:**
- Produces: CRUD `/api/products`, nested CRUD `/api/products/{productId}/variants`, `/attributes`, `/tags`, and read `/api/tags`.
- Consumes: existing Product/Variant/Tag/TagMapping/Attribute services and `ProductAuthorizationService`.

- [ ] **Step 1: Write failing MockMvc tests for complete Product surface**

```java
@Test
void storeUserMayReadButCannotUpdateProductOrVariant() throws Exception {
    mvc.perform(get("/api/products/{id}", productId).with(jwtUser(storeUserId)))
            .andExpect(status().isOk());
    mvc.perform(put("/api/products/{id}", productId).with(jwtUser(storeUserId))
                    .contentType(APPLICATION_JSON).content(validProductJson()))
            .andExpect(status().isForbidden());
    mvc.perform(post("/api/products/{id}/variants", productId).with(jwtUser(storeUserId))
                    .contentType(APPLICATION_JSON).content(validVariantJson()))
            .andExpect(status().isForbidden());
}
```

Add tests for Global+permission success, absent permission denial, list pagination delegation, tag link/unlink, and activate/deactivate.

- [ ] **Step 2: Run tests and verify RED**

Run: `./mvnw -q -Dtest=ProductControllerSecurityTest,ProductVariantControllerSecurityTest test`

Expected: `404`/missing controller failures.

- [ ] **Step 3: Implement thin authenticated controllers**

```java
@GetMapping
public Page<ProductDto> getList(Authentication auth,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) UUID brandId,
        @RequestParam(required = false) UUID collectionId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) String gender,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    authorization.requireRead(userId(auth), "PRODUCT_VIEW");
    return productService.getList(keyword, brandId, collectionId, categoryId, gender, status, pageable);
}

@PutMapping("/{id}")
public ProductDto update(Authentication auth, @PathVariable UUID id,
        @Valid @RequestBody ProductDto request) {
    authorization.requireMutation(userId(auth), "PRODUCT_UPDATE");
    return productService.update(id, request);
}
```

Apply the same pattern to Variant (`PRODUCT_VARIANT_*`), Tag (`TAG_*`), Attribute (Product mutation permission matching the operation), and tag mappings. Remove the blanket public `/api/products/**` matcher; if customer catalog access is required, permit only explicitly identified storefront GET routes instead of mutation routes.

- [ ] **Step 4: Run Product API and service tests**

Run: `./mvnw -q -Dtest=ProductControllerSecurityTest,ProductVariantControllerSecurityTest,ProductVariantServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit completed Product API**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductVariantController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductTagController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductAttributeController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/SecurityConfig.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductControllerSecurityTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductVariantControllerSecurityTest.java
git commit -m "feat(product): expose scoped product master APIs"
```

### Task 4: Cloudinary Product Image Lifecycle

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StorageService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CloudinaryStorageService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/StorageUploadResult.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/ProductImageUploadRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductImageController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/ProductImage.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/ProductImageDto.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/mapper/ProductImageMapper.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductImageService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V14__product_image_cloudinary_public_id.sql`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CloudinaryStorageServiceTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ProductImageServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductImageControllerSecurityTest.java`

**Interfaces:**
- Produces: `StorageUploadResult uploadImage(MultipartFile file, String folder)`, `void deleteImage(String publicId)`.
- Produces: multipart `POST /api/products/{productId}/variants/{variantId}/images`, `PUT /api/products/{productId}/variants/{variantId}/images/{imageId}/content`, `DELETE /api/products/{productId}/variants/{variantId}/images/{imageId}`, and `PATCH /api/products/{productId}/variants/{variantId}/images/{imageId}/primary`.
- Persists: `ProductImage.cloudinaryPublicId` and `ProductImage.imageUrl` from provider response only.

- [ ] **Step 1: Write failing file-validation and persistence tests**

```java
@Test
void rejectsSpoofedImageBeforeCallingCloudinary() {
    MockMultipartFile file = new MockMultipartFile(
            "file", "photo.png", "image/png", "not-a-png".getBytes(UTF_8));
    assertThatThrownBy(() -> service.uploadImage(file, "products/variants"))
            .isInstanceOf(BusinessException.class);
    verifyNoInteractions(cloudinary);
}

@Test
void uploadPersistsOnlyProviderUrlAndPublicId() {
    when(storage.uploadImage(file, "products/variants/" + variantId))
            .thenReturn(new StorageUploadResult("https://res.cloudinary.com/x/image/upload/a.png", "products/variants/a"));
    ProductImageDto result = service.upload(variantId, file, true, 0);
    assertThat(result.getCloudinaryPublicId()).isEqualTo("products/variants/a");
    verify(imageRepository).save(argThat(image -> image.getImageUrl().startsWith("https://")
            && image.getCloudinaryPublicId().equals("products/variants/a")));
}
```

Also test empty, oversized, wrong MIME, JPEG/PNG/WebP/GIF signatures, Cloudinary malformed response, DB-save compensation, and deletion by persisted public id.

- [ ] **Step 2: Run tests and verify RED**

Run: `./mvnw -q -Dtest=CloudinaryStorageServiceTest,ProductImageServiceTest,ProductImageControllerSecurityTest test`

Expected: compilation failure for the storage interface/public-id field.

- [ ] **Step 3: Add schema and model support**

```sql
ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_product_images_cloudinary_public_id
    ON product_images(cloudinary_public_id);
```

Update canonical `CREATE TABLE product_images` with `cloudinary_public_id VARCHAR(255)` and map it to `ProductImageDto`. New uploads require a nonblank returned public id while legacy rows remain readable during migration.

- [ ] **Step 4: Implement validated Cloudinary adapter and image orchestration**

```java
public record StorageUploadResult(String secureUrl, String publicId) {}

public interface StorageService {
    StorageUploadResult uploadImage(MultipartFile file, String folder);
    void deleteImage(String publicId);
}
```

Read at most the configured maximum plus one byte, verify allowlisted MIME and magic bytes, generate a UUID asset id, call Cloudinary with `resource_type=image`, `overwrite=false`, and a server-owned folder, then validate nonblank HTTPS `secure_url` and `public_id`. Translate provider errors to a generic `BusinessException` and never log credentials/provider payloads.

`ProductImageService.upload` locks the Variant, uploads, saves provider values, and compensates by deleting the new asset if persistence fails. Replace keeps the old id until the new record state is durable. Controller mutations call `ProductAuthorizationService.requireMutation(actor, "PRODUCT_VARIANT_UPDATE")`; reads use `PRODUCT_VARIANT_VIEW`.

- [ ] **Step 5: Run image tests and Product regressions**

Run: `./mvnw -q -Dtest=CloudinaryStorageServiceTest,ProductImageServiceTest,ProductImageControllerSecurityTest,ProductVariantServiceTest test`

Expected: PASS.

- [ ] **Step 6: Commit Cloudinary integration**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StorageService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CloudinaryStorageService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/StorageUploadResult.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/ProductImageUploadRequest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/ProductImageController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/ProductImage.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/ProductImageDto.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/mapper/ProductImageMapper.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductImageService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V14__product_image_cloudinary_public_id.sql backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/CloudinaryStorageServiceTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/ProductImageServiceTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/ProductImageControllerSecurityTest.java
git commit -m "feat(product): manage images through Cloudinary"
```

### Task 5: Store-Scoped Inventory Balance, Ledger, Adjustment, and Statistics

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/InventoryAdjustmentRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/InventoryStatisticsDto.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/StoreInventoryStatisticsDto.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/InventoryController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/InventoryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/InventoryBalanceRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/InventoryTransactionRepository.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/InventoryServiceTest.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/InventoryControllerSecurityTest.java`

**Interfaces:**
- Produces: actor-aware `getBalance`, `getBalances`, `getTransactions`, `adjust`, and `getStatistics`.
- Produces: `/api/inventory/balances`, `/balances/{storeId}/{variantId}`, `/transactions`, `/adjustments`, `/statistics`.
- Consumes: `AuthorizationService`, `UserScopeService`, existing locked balance mutation path.

- [ ] **Step 1: Write failing Store isolation tests**

```java
@Test
void storeListUsesResolvedStoreInRepositorySoTotalIsScoped() {
    when(userScopeService.resolveStoreId(actorId, null)).thenReturn(storeA);
    when(balanceRepository.search(storeA, null, null, pageable)).thenReturn(pageOf(balanceA));
    Page<InventoryBalanceDto> result = service.getBalances(actorId, null, null, null, pageable);
    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(balanceRepository).search(storeA, null, null, pageable);
}

@Test
void storeUserCannotGetKnownBalanceFromAnotherStore() {
    doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
            .when(userScopeService).requireStoreAccess(actorId, storeB);
    assertThatThrownBy(() -> service.getBalance(actorId, storeB, variantId))
            .isInstanceOf(BusinessException.class);
    verify(balanceRepository, never()).findById(any());
}
```

Add Global-all/filter tests, permission denial tests, adjustment actor ownership, transaction scoping, statistics Store/global breakdown, and invalid sort/page input tests.

- [ ] **Step 2: Run tests and verify RED**

Run: `./mvnw -q -Dtest=InventoryServiceTest,InventoryControllerSecurityTest test`

Expected: FAIL because existing methods trust caller Store ids and no Inventory controller/statistics exist.

- [ ] **Step 3: Implement service-level scope and adjustment**

```java
@Transactional(readOnly = true)
public Page<InventoryBalanceDto> getBalances(UUID actorId, UUID requestedStoreId,
        UUID variantId, Integer threshold, Pageable pageable) {
    requirePermission(actorId, "INVENTORY_VIEW");
    UUID effectiveStoreId = userScopeService.resolveStoreId(actorId, requestedStoreId);
    return balanceRepository.search(effectiveStoreId, variantId, threshold, pageable)
            .map(balanceMapper::toDto);
}
```

`adjust` accepts `quantityDelta`, rejects zero, locks the persisted balance, forbids negative resulting available stock, and appends an `ADJUST` ledger row with `createdBy=actorId`. No client-provided final balance or actor is accepted.

- [ ] **Step 4: Add database aggregate projections**

Repository aggregate queries take `effectiveStoreId` and return total SKU, available, reserved, damaged, low-stock, and out-of-stock counts. A Global query may additionally group by `storeId`; Store users receive a single Store result. Do not calculate statistics by loading all balance entities.

- [ ] **Step 5: Implement thin Inventory controller and run tests**

Run: `./mvnw -q -Dtest=InventoryServiceTest,InventoryControllerSecurityTest test`

Expected: PASS, including direct-ID cross-Store denial and scoped totals.

- [ ] **Step 6: Commit Inventory scope**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/InventoryAdjustmentRequest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/InventoryStatisticsDto.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/StoreInventoryStatisticsDto.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/InventoryController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/InventoryService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/InventoryBalanceRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/InventoryTransactionRepository.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/InventoryServiceTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/InventoryControllerSecurityTest.java
git commit -m "feat(inventory): enforce store scoped stock access"
```

### Task 6: Store-Scoped Import and Export Receipts

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/CreateGoodsReceiptRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/UpdateGoodsReceiptRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/CreateGoodsIssueRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/UpdateGoodsIssueRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/GoodsReceiptController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/GoodsIssueController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsReceiptService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsIssueService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/GoodsReceiptScopeTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/GoodsIssueScopeTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/GoodsReceiptControllerSecurityTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/GoodsIssueControllerSecurityTest.java`

**Interfaces:**
- Produces: CRUD, items, and approval APIs under `/api/import-receipts` and `/api/export-receipts`.
- Consumes: `UserScopeService`, `AuthorizationService`, actor-aware Inventory service.

- [ ] **Step 1: Write failing cross-Store receipt tests**

```java
@Test
void createRejectsStoreBFromStoreAUserAndUsesAuthenticatedActor() {
    when(userScopeService.resolveStoreId(actorId, storeB))
            .thenThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"));
    assertThatThrownBy(() -> service.create(actorId, requestFor(storeB)))
            .isInstanceOf(BusinessException.class);
    verify(receiptRepository, never()).save(any());
}

@Test
void approveChecksPersistedReceiptStoreBeforeInventoryMutation() {
    when(receiptRepository.findByIdForUpdate(receiptId)).thenReturn(Optional.of(receiptAt(storeB)));
    doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
            .when(userScopeService).requireStoreAccess(actorId, storeB);
    assertThatThrownBy(() -> service.approve(actorId, receiptId))
            .isInstanceOf(BusinessException.class);
    verifyNoInteractions(inventoryService);
}
```

Repeat for Issue detail, update, delete, item add/update/delete, list totals, and approval.

- [ ] **Step 2: Run tests and verify RED**

Run: `./mvnw -q -Dtest=GoodsReceiptScopeTest,GoodsIssueScopeTest,GoodsReceiptControllerSecurityTest,GoodsIssueControllerSecurityTest test`

Expected: compilation or assertion failures because services have no actor-aware authorization.

- [ ] **Step 3: Implement request DTOs and Store-scoped services**

Create DTOs omit `approvedBy`, server-owned totals/status/timestamps, and any trusted actor field. `create(actorId, request)` resolves the effective Store and sets `receivedBy`/`issuedBy` to the authenticated actor. Every detail/mutation loads persisted parent Store then calls `requireStoreAccess` before child access or writes. Every list resolves Store before calling existing repository search.

- [ ] **Step 4: Implement controllers with exact permissions**

Use `IMPORT_RECEIPT_VIEW/CREATE/UPDATE/DELETE/APPROVE` and `EXPORT_RECEIPT_VIEW/CREATE/UPDATE/DELETE/APPROVE`. Controllers pass the authenticated actor id and never accept actor ids.

- [ ] **Step 5: Run receipt, issue, and Inventory regression tests**

Run: `./mvnw -q -Dtest=GoodsReceiptScopeTest,GoodsIssueScopeTest,GoodsReceiptControllerSecurityTest,GoodsIssueControllerSecurityTest,InventoryServiceTest test`

Expected: PASS; approval updates document and stock in one transaction.

- [ ] **Step 6: Commit receipt scope**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/CreateGoodsReceiptRequest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/UpdateGoodsReceiptRequest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/CreateGoodsIssueRequest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/UpdateGoodsIssueRequest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/GoodsReceiptController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/GoodsIssueController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsReceiptService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsIssueService.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/GoodsReceiptScopeTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/GoodsIssueScopeTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/GoodsReceiptControllerSecurityTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/GoodsIssueControllerSecurityTest.java
git commit -m "feat(inventory): scope import and export receipts"
```

### Task 7: Frontend Scope Context and Product Read-Only Behavior

**Files:**
- Create: `frontend/react-app/src/hooks/userScope.js`
- Modify: `frontend/react-app/src/contexts/AdminPermissionsContext.jsx`
- Modify: `frontend/react-app/src/hooks/usePermissions.js`
- Modify: `frontend/react-app/src/pages/admin/ProductItem/ProductManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ProductItem/VariantManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ProductItem/BrandManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ProductItem/CategoryManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ProductItem/CollectionManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ProductItem/TagManagement.jsx`
- Modify: relevant dialogs under `frontend/react-app/src/components/admin/Product`, `ProductVariant`, `Brand`, `Category`, `Collection`, and `Tag` only where read-only mode must disable inputs/submits.

**Interfaces:**
- Consumes: `GET /api/me/scope`, existing `/api/me/permissions`.
- Produces: `isGlobal`, `isStore`, `currentStore`, and `canMutateProduct(permissionCode)` in the permission context.

- [ ] **Step 1: Add a pure decision helper with an initially failing lint/import check**

```javascript
export function canMutateProduct(scope, hasPermission, permissionCode) {
  return scope?.scope === "GLOBAL" && hasPermission(permissionCode);
}
```

Wire one Product page to import the nonexistent helper, then run lint to establish RED.

- [ ] **Step 2: Run frontend lint and verify RED**

Run: `npm run lint`

Expected: module resolution failure for `hooks/userScope.js`.

- [ ] **Step 3: Implement scope loading and shared mutation decision**

Load permissions and scope together, abort both on unmount, expose loading/error consistently, and never derive Global status from role names. A scope-load failure defaults mutation controls to hidden.

- [ ] **Step 4: Apply read-only behavior without redesigning pages**

For every Product master page, keep View/search/filter/pagination controls for Store users. Hide Add/Delete/Activate/Deactivate. Edit dialogs opened in view mode render disabled fields and no submit. Global users see each action only when `canMutateProduct` receives the matching permission.

- [ ] **Step 5: Run lint and production build**

Run: `npm run lint`

Run: `npm run build`

Expected: PASS.

- [ ] **Step 6: Commit frontend Product scope behavior**

```bash
git add frontend/react-app/src/hooks/userScope.js frontend/react-app/src/contexts/AdminPermissionsContext.jsx frontend/react-app/src/hooks/usePermissions.js frontend/react-app/src/pages/admin/ProductItem frontend/react-app/src/components/admin/Product frontend/react-app/src/components/admin/ProductVariant frontend/react-app/src/components/admin/Brand frontend/react-app/src/components/admin/Category frontend/react-app/src/components/admin/Collection frontend/react-app/src/components/admin/Tag
git commit -m "feat(frontend): make product master read only for store users"
```

### Task 8: Frontend Product/Inventory APIs and Server Pagination

**Files:**
- Create: `frontend/react-app/src/hooks/productInventoryApi.js`
- Modify: `frontend/react-app/src/hooks/auth/adminSession.js`
- Modify: `frontend/react-app/src/pages/admin/InventoryManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ImportReceiptManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/ExportReceiptManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/StatisticsManagement.jsx`
- Modify: `frontend/react-app/src/components/admin/ProductVariant/ProductVariantImageSection.jsx`
- Modify: `frontend/react-app/src/components/admin/Product/ProductImageSection.jsx`

**Interfaces:**
- Consumes: Product/Inventory/receipt/issue/statistics endpoints and multipart image endpoint.
- Produces: API helpers using `requestAdmin`; adds a FormData-capable request option without forcing JSON content type.

- [ ] **Step 1: Establish RED by wiring Inventory to the missing API client**

Replace the mock source import with `inventoryApi.list` and run build.

Run: `npm run build`

Expected: module resolution failure for `hooks/productInventoryApi.js`.

- [ ] **Step 2: Implement API client and multipart support**

```javascript
export const inventoryApi = {
  list: (params, options = {}) => requestAdmin(`/api/inventory/balances${queryString(params)}`, options),
  statistics: (storeId, options = {}) => requestAdmin(
    `/api/inventory/statistics${queryString({ storeId })}`, options),
  adjust: (body) => requestAdmin("/api/inventory/adjustments", { method: "POST", body }),
};
```

Extend `requestAdmin` so `FormData` is sent unchanged and browser-generated multipart boundaries are preserved. JSON remains the default for plain objects.

- [ ] **Step 3: Replace Inventory mock/local pagination with server state**

Drive rows, `totalElements`, total pages, and cards from API responses. Debounce search, send page/size/sort/filter parameters, and render loading/error/empty states. Global users receive Store filter/column. Store users send no alternative Store id, see the server-returned Store context in the header, and never receive an editable Store selector.

- [ ] **Step 4: Connect receipt/issue pages and Inventory statistics section**

Use server pagination and scoped endpoints for receipt/issue list/detail/mutations. Scope Store selectors exactly like Inventory. Replace only Inventory-related mock cards in `StatisticsManagement`; preserve unrelated reporting widgets.

- [ ] **Step 5: Connect Product image upload**

Submit `FormData` containing only file, primary flag, and sort order. Render returned `imageUrl`; never let the frontend choose `cloudinaryPublicId` or trusted URL. Hide image mutation controls for Store users.

- [ ] **Step 6: Run lint and build**

Run: `npm run lint`

Run: `npm run build`

Expected: PASS.

- [ ] **Step 7: Commit connected frontend module**

```bash
git add frontend/react-app/src/hooks/productInventoryApi.js frontend/react-app/src/hooks/auth/adminSession.js frontend/react-app/src/pages/admin/InventoryManagement.jsx frontend/react-app/src/pages/admin/ImportReceiptManagement.jsx frontend/react-app/src/pages/admin/ExportReceiptManagement.jsx frontend/react-app/src/pages/admin/StatisticsManagement.jsx frontend/react-app/src/components/admin/ProductVariant/ProductVariantImageSection.jsx frontend/react-app/src/components/admin/Product/ProductImageSection.jsx
git commit -m "feat(frontend): connect scoped inventory and image APIs"
```

### Task 9: Full Verification and Security Review

**Files:**
- Modify only files proven necessary by failures introduced by Tasks 1-8.
- Review: all files listed in Tasks 1-8.

**Interfaces:**
- Verifies every spec rule and preserves existing callers.

- [ ] **Step 1: Run focused authorization matrix**

Run: `./mvnw -q -Dtest=UserScopeServiceTest,ProductAuthorizationServiceTest,ProductMasterControllerSecurityTest,ProductControllerSecurityTest,ProductVariantControllerSecurityTest,ProductImageControllerSecurityTest,InventoryControllerSecurityTest,GoodsReceiptControllerSecurityTest,GoodsIssueControllerSecurityTest test`

Expected: PASS for Global allowed-by-RBAC, Store Product mutation denied despite permission, Store A cross-Store denial, and Global Inventory filters.

- [ ] **Step 2: Run full backend tests**

Run: `./mvnw test -q`

Expected: PASS. If a pre-existing unrelated failure occurs, confirm it against the base state and report it separately; fix all regressions caused by this implementation.

- [ ] **Step 3: Run clean backend compile**

Run: `./mvnw -q -DskipTests clean compile`

Expected: exit code 0.

- [ ] **Step 4: Run frontend verification**

Run: `npm run lint`

Run: `npm run build`

Expected: exit code 0 for both. There is no separate TypeScript script in this JavaScript Vite project.

- [ ] **Step 5: Review the final diff and schema safety**

Run: `git diff --check`

Run: `git status --short`

Confirm no unrelated dirty files were staged or rewritten, no secret was added, Product/Variant gained no Store field, every Inventory repository call is scoped before query, and Cloudinary ids only originate from provider responses.

- [ ] **Step 6: Commit verification-only fixes if any**

Review `git status --short`, stage each regression-fix file by its explicit path, and commit with `git commit -m "fix: complete product inventory verification"`. If no fixes are needed, do not create an empty commit.
