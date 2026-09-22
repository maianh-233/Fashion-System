# Product catalog media, notes, and seed data

## Scope

This change is limited to the product-management module: brands, collections,
products, product variants, and the category dependency used by product forms.
It adds reusable UI for catalog cover images and long notes/descriptions,
repairs category activation compatibility, and provides a deliberate reset-and-
seed SQL script for catalog data.

## Shared dialog components

`CatalogImageField` becomes a dedicated reusable catalog image component.  It
accepts the current remote URL and a local selected file, validates JPEG, PNG,
GIF, and WebP files up to 25 MB, safely creates/revokes an object URL, and
renders a preview. In create/edit mode it offers image selection and removal;
in detail mode it is a read-only preview. The component returns the selected
file to `ApiCatalogPage`, which continues to save the master record first and
then uses the existing protected upload endpoint.

`CatalogNoteField` is a reusable long-text component. In create/edit mode it
uses a labelled textarea with a character counter and preserves line breaks.
In detail mode it renders a readable, pre-wrapped block, clamps long content,
and exposes an accessible `Xem thêm` / `Thu gọn` control. Empty notes show a
clear dash rather than unused form space.

`ApiCatalogPage` selects those components using the existing `image` and
`textarea` field types. Therefore Brand, Collection, and Product dialogs get
identical behavior for add/edit/detail without each page reimplementing it.
Product variant media stays in `ProductImageSection`: variants own multiple
images, so replacing it with a single cover-image picker would lose behavior.

## Data and migration

The observed SQL error is caused by a deployed database whose `categories`
table predates `V18__catalog_master_rules.sql`; the JPA entity and repository
already require `categories.active`. A new idempotent Flyway migration will:

1. add `categories.active` if absent;
2. backfill null values to `TRUE` and enforce `NOT NULL DEFAULT TRUE`;
3. create the active lookup index if needed.

This makes upgraded and partially provisioned PostgreSQL databases compatible
without relying on manual schema edits.

## Reset and demo data

A PostgreSQL-only SQL script will be supplied outside Flyway migrations, so it
is always an explicit operator action. It deletes catalog records in foreign-
key-safe order, resets the catalog sequences, then inserts coherent Vietnamese
fashion sample data: active/inactive brands, parent/child categories,
collections, products, size/color variants, primary variant images, tags and
product-tag mappings. IDs are captured with CTEs/known values so every
relationship is valid; status, price, image URLs and timestamps are valid for
the current API contract.

The script will state that it destroys catalog data and must not be run against
production data unless this replacement is intentional.

## Error handling and verification

Unit tests will cover file validation/removal and long-note expansion, along
with a backend migration-compatible category repository path or schema test as
the project test layout permits. Verification includes the focused frontend
test suite/build and focused backend tests, plus reviewing the SQL for FK order
and applying it against a disposable PostgreSQL database when available.
