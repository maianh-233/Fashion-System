# Product Catalog Media and Seed Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give catalog dialogs consistent image and long-note UX, repair category activation upgrades, and provide clean catalog demo data.

**Architecture:** Extract image and long-note controls from `ApiCatalogPage` into focused React components while retaining its protected upload lifecycle. Add an idempotent Flyway migration after V18; supply destructive demo reset separately from Flyway.

**Tech Stack:** React 19, Vite, Tailwind CSS, Spring Boot 4, JPA, Flyway, PostgreSQL.

**Spec:** `docs/superpowers/specs/2026-09-22-product-catalog-media-and-seed-design.md`

## Global Constraints

- Limit UI changes to brand, collection, product, variant, and category flows.
- Reuse protected upload APIs; image inputs never submit arbitrary URLs.
- Images: JPEG, PNG, GIF, and WebP; maximum 25 MiB.
- Retain `ProductImageSection` for variant multi-image handling.
- Reset data is an explicit destructive PostgreSQL operator script, never a Flyway migration.

## Review Focus

- A valid file after a rejected one clears errors and allows saving.
- Replacing or clearing images releases object URLs and strips `imageFile` from DTOs.
- Long multiline notes are readable and expand/collapse in detail mode.
- Missing/null `categories.active` becomes queryable after migration.
- Deletion/insertion ordering satisfies catalog foreign keys.

---

### Task 1: Shared catalog image and note fields

**Files:**
- Create `frontend/react-app/src/components/admin/catalog/catalogFieldLogic.js`
- Create `frontend/react-app/src/components/admin/catalog/CatalogImageField.jsx`
- Create `frontend/react-app/src/components/admin/catalog/CatalogNoteField.jsx`
- Modify `frontend/react-app/src/components/admin/catalog/ApiCatalogPage.jsx`
- Test `frontend/react-app/src/components/admin/catalog/catalogFieldLogic.test.js`

**Interfaces:** `validateCatalogImage(file): string`; `shouldClampNote(value): boolean`; `CatalogImageField({ file, currentUrl, disabled, onChange })`.

- [ ] Write the failing test with the following assertions.

```js
assert.match(validateCatalogImage({ type: "image/svg+xml", size: 1 }), /JPEG/);
assert.match(validateCatalogImage({ type: "image/png", size: 25 * 1024 * 1024 + 1 }), /25 MB/);
assert.equal(validateCatalogImage({ type: "image/webp", size: 1 }), "");
assert.equal(shouldClampNote("x".repeat(501)), true);
```

- [ ] Run `node --test src/components/admin/catalog/catalogFieldLogic.test.js` in `frontend/react-app`; it must fail because the module is absent.
- [ ] Implement `validateCatalogImage` with allowed MIME types and 25 MiB limit; implement the 500-character note clamp. Implement preview cleanup/removal in the image control. Implement textarea plus counter when editable and `whitespace-pre-wrap` plus `Xem thêm`/`Thu gọn` when read-only.
- [ ] Replace local `CatalogImageField` in `ApiCatalogPage` and render `CatalogNoteField` for `textarea` fields.
- [ ] Re-run the test and `npm run build`; both must pass.
- [ ] Commit with message `feat(catalog): share image and note dialog fields`.

### Task 2: Catalog page integration contract

**Files:**
- Test `frontend/react-app/src/pages/admin/ProductItem/catalogManagement.contract.test.js`
- Verify `BrandManagement.jsx`, `CollectionManagement.jsx`, `ProductManagement.jsx`, `VariantManagement.jsx`

**Interfaces:** Brand, collection, and product retain `image` and `textarea` descriptors; variant retains `ProductImageSection`.

- [ ] Add a source-contract test that asserts all three masters contain `type: "image"` and `type: "textarea"`, while variant contains `ProductImageSection`.
- [ ] Run `node --test src/pages/admin/ProductItem/catalogManagement.contract.test.js`; it must pass because descriptors are the existing integration seam.
- [ ] Make compatibility-only page edits if the extracted components require them; never introduce a single cover image to variants.
- [ ] Run `npm run build` in `frontend/react-app`; commit as `test(catalog): cover dialog field integration`.

### Task 3: Category activation schema compatibility

**Files:**
- Create `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V19__ensure_category_active_column.sql`
- Test `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/db/migration/CatalogMigrationScriptTest.java`

**Interfaces:** migration produces `categories.active BOOLEAN NOT NULL DEFAULT TRUE` and `idx_categories_active` required by JPA searches.

- [ ] Add a failing script-content test requiring `ADD COLUMN IF NOT EXISTS active BOOLEAN`, `SET active = TRUE WHERE active IS NULL`, `SET NOT NULL`, and `CREATE INDEX IF NOT EXISTS idx_categories_active`.
- [ ] Run `mvn -q -Dtest=CatalogMigrationScriptTest test` in `backend/fashion-system`; it must fail because V19 is absent.
- [ ] Add the idempotent migration: add nullable column if absent, backfill nulls, set default, set not null, create index.
- [ ] Re-run the focused test and `mvn -q -DskipTests compile`; both must pass.
- [ ] Commit as `fix(catalog): backfill category active schema`.

### Task 4: Catalog reset and seeded demo data

**Files:**
- Create `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/reset_catalog_demo_data.sql`
- Test `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/db/migration/CatalogSeedScriptTest.java`

**Interfaces:** manual PostgreSQL transaction deletes associations before masters, resets sequences, and inserts coherent relationships.

- [ ] Add a failing test asserting the script contains `BEGIN;`, deletes product images, variants, products, categories, and ends in `COMMIT;`.
- [ ] Run `mvn -q -Dtest=CatalogSeedScriptTest test`; it must fail because the script is absent.
- [ ] Implement a transaction script with an explicit destructive warning, FK-safe deletes, sequence resets, then realistic brands, parent/child categories, collections, products, variants, images, tags, and mappings. Use fixed UUIDs so all relationships are valid and prices/statuses match entity constraints.
- [ ] Re-run the seed test and inspect each referenced table/column against entity annotations; commit as `docs(catalog): add resettable product demo data`.

### Task 5: Final verification

**Files:** all files modified by Tasks 1–4.

- [ ] Run the two frontend node tests and `npm run build` from `frontend/react-app`.
- [ ] Run `mvn -q -Dtest=CatalogMigrationScriptTest,CatalogSeedScriptTest test` and `mvn -q -DskipTests compile` from `backend/fashion-system`.
- [ ] Run `git diff --check` and `git status --short`; only intentional catalog files may remain.
