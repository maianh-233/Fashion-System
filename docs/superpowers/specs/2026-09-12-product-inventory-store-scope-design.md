# Product and Inventory Store Scope Design

## Purpose

Secure and connect the existing Product and Inventory modules for a multi-store clothing chain. Product master data remains global, inventory remains owned by Store, and backend authorization combines effective RBAC permissions with a separately resolved user data scope.

## Current Architecture

- Spring Boot 4.1 / Java 21 backend with Spring Security method authorization, PostgreSQL, JPA, Redis cache support, and Cloudinary client configuration.
- React 19 / Vite frontend with an in-memory employee access token, HttpOnly refresh flow, and an effective-permission context.
- Users are assigned to stores through active `StoreStaff` records.
- RBAC grants actions and includes `PermissionScope`, but current Product controllers do not check action permissions and Product/Inventory APIs are incomplete.
- Inventory ownership is already explicit through `(store_id, product_variant_id)` on `InventoryBalance`; receipts, issues, transactions, reservations, and orders also carry `store_id`.
- Inventory mutations already use a PostgreSQL upsert followed by `PESSIMISTIC_WRITE` inside a transaction.
- Product detail caching already exists behind an opt-in, resilient Redis cache.
- Product images currently persist only `image_url`; Cloudinary configuration exists but no storage adapter or secured upload endpoint exists.
- The current admin Product and Inventory screens primarily use local mock data and client-side filtering.

## Scope Classification

Create a shared `UserScopeService` whose result is independent from role names and independent from the scope accidentally granted to a particular permission.

- No active `StoreStaff` assignment: `GLOBAL`.
- Active assignments resolving to exactly one distinct active Store: `STORE`, with that Store's id, code, and name.
- Active assignments for multiple distinct Stores: reject with `403` because the identity scope is ambiguous in the current single-store employee model.
- An assignment to a missing or inactive Store: reject with `403`.

This identity scope is an additional restriction. Effective RBAC still answers whether the user may perform an action. A permission with `ALL` scope cannot turn a Store user into a Global user.

`EmployeeDataScopeService` will reuse this resolver where compatible so Store identity rules do not diverge between employee, Product, and Inventory modules.

Expose `GET /api/me/scope` for frontend presentation only. It derives the user id from the authenticated principal and accepts no client-provided user id.

## Authorization Model

Every protected operation requires both layers:

1. `AuthorizationService` verifies the effective permission code after role and user overrides.
2. `UserScopeService` constrains the data or requires Global identity.

Controllers extract only the authenticated user id. Services perform the business authorization and resource-scope checks so direct service entry paths cannot bypass the policy. No business decision depends on role names.

Missing action permission returns `403`. Cross-store access returns `403` consistently, including when the caller knows a record id. Invalid or absent resources inside an allowed scope return `404`.

## Product Master Data

Product master entities include Brand, Category, Collection, Product, ProductVariant, ProductTag, ProductTagMapping, ProductAttribute, and ProductImage.

- Read operations require their existing view permission and return the same global catalog for Global and Store users.
- Create, update, delete, activate/deactivate, primary-image changes, SKU/barcode changes, and relationship mutations require both the matching RBAC permission and Global identity.
- Store users are denied mutations even if the database grants a mutation permission with `ALL` scope.
- Product and Variant do not receive a Store foreign key.
- Product list search, filters, sorting, and pagination remain database-side.

Add the missing Product, Variant, Tag, Attribute, Image, and tag-mapping HTTP surfaces using the existing services and endpoint naming conventions. Existing Brand, Category, and Collection controllers receive explicit RBAC plus Global-scope enforcement.

## Store-Scoped Inventory

Inventory continues using Store ownership already present in the schema; no Warehouse relation or redundant Store field is introduced.

For Inventory, GoodsReceipt, GoodsIssue, InventoryTransaction, and relevant StockReservation operations:

- A Global user with the required permission may omit `storeId` to query all Stores or supply a Store filter.
- A Store user with the required permission is always constrained to the Store resolved by `UserScopeService`.
- If a Store user supplies another `storeId`, the request is rejected with `403`; it is not silently trusted.
- Detail and mutation operations load the resource, compare its persisted `storeId` with the resolved Store, and reject cross-store access.
- Child resources inherit scope through their persisted parent receipt/issue/reservation.
- List queries pass the effective Store id into repository queries, so `totalElements`, statistics, and pagination counts are computed inside the allowed Store.
- Actor fields such as `createdBy`, `receivedBy`, `issuedBy`, and `approvedBy` come from the authenticated principal where they represent the acting user; request values cannot impersonate another user.

Add Inventory balance and transaction endpoints plus receipt/issue endpoints around the existing services. Stock adjustment uses a dedicated validated request and the locked balance row; callers cannot submit a resulting balance.

## Product Detail Inventory Projection

Product and Variant information remains global. An authenticated internal Product detail response may include an inventory section determined by user scope:

- Store user: only the current Store's balances and Store identity.
- Global user with `INVENTORY_VIEW`: balances across Stores, optionally filtered by Store.
- User without `INVENTORY_VIEW`: no inventory data.

The general Product read permission never implicitly grants cross-store stock visibility.

## Statistics and Dashboard

Inventory summary queries aggregate at the database using the effective Store id.

- Store response covers only its Store and includes that Store context.
- Global response can cover all Stores and provide per-Store breakdown when the caller has the required permission.
- Counts and low/out-of-stock totals are never calculated from an unscoped in-memory collection.

The existing statistics page contains extensive mock analytics outside the Inventory scope. This change connects and scopes the Inventory cards/section without redesigning unrelated reporting.

## Cloudinary Product Images

Add a reusable storage adapter around the configured Cloudinary client. Product image upload is multipart and server-mediated.

Validation before upload:

- Reject empty files.
- Enforce configured maximum bytes.
- Accept only an explicit image MIME allowlist.
- Verify file signature/content rather than trusting the multipart MIME type, extension, filename, URL, or public id.
- Generate the destination folder and asset name on the server.

The adapter returns the Cloudinary `secure_url` and `public_id`. Add a non-null `cloudinary_public_id` column for new Product images, update entity/DTO/mapper, and provide a forward migration plus the canonical schema update. Existing rows are handled by a nullable migration transition; application validation requires public id for new uploads.

Create/update image requests do not accept arbitrary `imageUrl` or `cloudinaryPublicId`. On successful upload, persist only values returned by Cloudinary. If DB persistence fails after upload, attempt compensating asset deletion. For image deletion, remove the database association transactionally and invoke Cloudinary through a controlled service path; failures are translated without leaking provider details. Replacing an image uploads the new asset first and only deletes the old asset after the new association is safely persisted.

Only Global users with the relevant Product Variant mutation permission can upload, replace, delete, or set primary images. Image reads follow Product/Variant read authorization.

## Redis and Kafka

Retain the existing opt-in Spring Cache configuration for Product master detail caches and its failure fallback. Ensure every changed master-data mutation invalidates or updates its cache. Do not make Redis a source of truth and do not add inventory authorization or stock state to Redis.

Do not add inventory-statistics caching initially because no measured heavy query exists. Do not add Kafka because the repository has no established event consumers, outbox, or required asynchronous use case. Stock and image business success remains synchronous and database-backed.

## Inventory Consistency

Keep the existing database transaction and `PESSIMISTIC_WRITE` row lock for stock changes. The current conflict-safe balance initialization plus row lock prevents lost updates across application instances sharing PostgreSQL. Append the inventory ledger entry in the same transaction as the balance change. Redis locks are not introduced.

Receipt and issue approval lock their document row and call Inventory mutation methods within the same transaction. Reapproval and insufficient-stock cases remain rejected before committing.

## Frontend Behavior

Extend the existing admin auth/permission contexts with the server-derived `/api/me/scope` response.

Global users:

- See Product mutation controls only when the matching permission is present.
- See Inventory Store filter and Store column.
- Can request cross-store inventory statistics when permitted.

Store users:

- See Product, Variant, Brand, Category, Collection, Tag, Attribute, and image screens in read-only mode.
- Never see active mutation controls even if mutation permissions appear in the permission tree.
- See only current-Store Inventory data and statistics.
- Do not receive an editable Store selector; the page header shows the Store name.

Replace mock/local Inventory data with server-paginated requests through the existing `requestAdmin` abstraction. Product pages keep current visual styling and components while using shared helpers for `canMutateProduct = isGlobal && hasPermission(code)`. Loading, error, empty, and disabled-submit states follow existing UI conventions.

Frontend checks improve UX only. Backend authorization is authoritative.

## API and Input Safety

- Use validated request DTOs rather than accepting persistence DTOs for security-sensitive create/update operations where server-owned fields exist.
- Whitelist sort fields and enum-like filters.
- Normalize strings server-side and validate referenced ids.
- Bound page sizes to prevent unbounded reads.
- Never accept actor user ids or trusted Store ownership from the client.
- Return centralized `BusinessException` responses without provider, SQL, credential, or stack-trace details.

## Schema Changes

- Add `product_images.cloudinary_public_id` with an index suitable for provider lifecycle lookup.
- Do not add Store columns to Product or ProductVariant.
- Do not change the Inventory composite ownership key.
- Add only indexes justified by scoped Inventory/statistics repository queries after checking the existing schema.

## Testing

Backend tests cover:

- Scope resolution for no Store, one Store, inactive Store, and ambiguous multiple Stores.
- Store user with Product mutation permission still receives `403` for Product, Variant, Brand, Category, Collection, Tag, Attribute, and image mutations.
- Both user types can read Product master data when RBAC permits.
- Store A list totals, detail, adjustment, transactions, receipt/issue detail, child mutations, approval, and statistics cannot reach Store B.
- Store A request containing Store B is rejected.
- Global Inventory user can query all Stores and filter one Store when RBAC permits.
- Stock mutation remains atomic, locked, and rejects invalid/insufficient quantities.
- Multipart image validation rejects spoofed/oversized/non-image content; successful upload persists Cloudinary response public id and URL; compensation/deletion paths are covered with provider mocks where external calls are unavoidable.
- Controller security tests assert `401`, `403`, and allowed status behavior.

Frontend verification covers lint and production build. Where the repository has no frontend test runner, scope/mutation decisions are kept in small pure helpers and validated through lint/build rather than adding a new testing framework solely for this change.

## Delivery Boundaries

- Preserve unrelated dirty-worktree changes.
- Do not redesign the admin UI.
- Do not introduce Redis locks, Kafka, a second authorization framework, Warehouse entities, or Product-to-Store ownership.
- Do not attempt to convert every existing mock reporting widget outside Product/Inventory scope.
- Report any pre-existing build/test failures separately from regressions introduced by this work.
