# Store warehouse migration (V22)

The backend does not configure Flyway. Shipping the SQL resource alone **does not apply the migration**. Apply `backend/fashion-system/src/main/resources/db/migration/V22__store_warehouse_workflow.sql` explicitly before starting the updated backend. This is an upgrade of the existing database through V21, including the authorization seed; it is not a fresh database bootstrap.

## Apply

1. Stop application instances and other stock/receipt writers. Back up the target database using your normal `pg_dump` procedure and verify that the backup is usable.
2. Confirm the database, schema, and environment. Use your existing PostgreSQL connection configuration (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and a password prompt or password file). Do not paste passwords into command history.
3. From the repository root, run:

   ```powershell
   psql -X -v ON_ERROR_STOP=1 -c "SELECT current_database(), current_schema(), current_user;"
   psql -X -v ON_ERROR_STOP=1 -f backend/fashion-system/src/main/resources/db/migration/V22__store_warehouse_workflow.sql
   ```

   Tables are resolved through the configured `search_path` (normally `public`). For a custom schema, configure the connection's search path before running. The file contains its own `BEGIN`/`COMMIT`; do not wrap it in an outer transaction. A lock failure, failed preflight, or invalid existing data aborts the entire migration. Reconcile the reported records and rerun; do not delete stock or receipt history to make validation pass.
4. Review the new permission assignments below, start the updated application, and verify a draft → pending confirmation → confirmed → completed receipt in the intended store. Confirmation must not move stock; completion must move stock once. Keep the old application stopped because it understands the old receipt states.

## Existing data and authorization

- `available_quantity` remains the physical SQL column for **offline** stock. Online stock starts at zero. Reserved and damaged stock are preserved. Total is computed as offline + online; no duplicated total column is added. Both buckets are nonnegative and their sum must fit a Java signed integer. The composite balance primary key remains `(store_id, product_variant_id)`.
- `PENDING` becomes `DRAFT`; `APPROVED` becomes `COMPLETED`, because approval already changed stock in the old workflow. No stock movements are replayed. Legacy `approved_by` becomes the completer; `updated_at` supplies confirmation/completion timestamps when available. Missing historical timestamps remain null. Existing DRAFT/PENDING_CONFIRMATION/CONFIRMED/COMPLETED/CANCELLED states survive reruns. Unknown statuses fail preflight and require explicit review.
- Import supplier columns remain nullable to preserve legacy completed imports and editable drafts. Database checks require a supplier for future submitted/confirmed/completed imports and supplier returns; OTHER exports in these states require a nonblank reason. These checks, along with positive item quantities and nonnegative import costs, use `NOT VALID` so existing invalid history survives; all future inserts/updates are checked. Do not blindly validate these constraints until historical exceptions have been reconciled. The application additionally validates transition requirements. Historical creators and approvers remain unchanged. Invalid historical completer IDs or orphan variants cause an atomic failure and require reconciliation.
- Item `product_id` is backfilled from its variant, then constrained to the exact product/variant pair. Existing item channels default to OFFLINE.
- Supported export types are `RETURN_TO_SUPPLIER`, `DAMAGED`, `OTHER`, `ONLINE_TO_OFFLINE`, and `OFFLINE_TO_ONLINE`. Other legacy types become OTHER; their original type is appended to `reason`. Existing order links and notes are retained.
- Existing ledger rows keep their generic reference IDs. New before/after bucket snapshots and typed receipt FKs remain null for legacy rows because historical online state and generic reference provenance cannot safely be reconstructed. New movements fill these columns.
- CONFIRM, COMPLETE, and CANCEL permissions are added for both receipt groups. Existing APPROVE grants are copied to CONFIRM; DELETE grants are copied to CANCEL. Role scope and user ALLOW/DENY overrides are copied. If the target permission already has a grant/override, that existing assignment wins. Old permission records remain for audit compatibility, but updated receipt endpoints use the new action rights.
- COMPLETE is granted only to an existing role with code ADMIN. Other roles, including WAREHOUSE, require an administrator to assign COMPLETE explicitly with the intended store scope. This deliberately separates confirmation from stock-changing completion. Missing required permission groups abort the migration instead of silently omitting the permissions.

## Verification

Tests use a minimal pre-V22 fixture modeled on the old schema and a disposable PostgreSQL database. They do **not** claim that a live application database was migrated. A fresh database should be created specifically for the runner; its name must begin with `warehouse_`. Example for an isolated localhost PostgreSQL instance:

```powershell
createdb -h 127.0.0.1 -p 55432 -U warehouse_test warehouse_migration_test
python backend/fashion-system/scripts/verify_warehouse_migration.py --host 127.0.0.1 --port 55432 --user warehouse_test --database warehouse_migration_test
```

Use `--psql` if the executable is not on PATH. The runner creates a uniquely named schema, runs its tests there, and removes that schema in a `finally` block. It checks:

- Successful upgrade and harmless second application, with preserved stock and legacy history.
- Scoped grants and direct DENY preservation; no implicit warehouse COMPLETE grant.
- Every lifecycle state including PENDING_CONFIRMATION, submit → confirm persistence, and migration rerun with a submitted receipt.
- Rejection of negative/overflow stock, nonpositive item quantities, negative import costs, missing submitted suppliers/reasons, invalid channels/states, mismatched variant/product pairs, and orphan typed references.
- Atomic rollback on both early unsupported-state failure and late orphan-item failure, including rollback of status changes and DDL.
- Two concurrent PostgreSQL sessions exporting 8 and 5 from stock 10 using row locks: exactly one succeeds and stock remains nonnegative.
- Concurrent first-balance insertion with `ON CONFLICT DO NOTHING` and `FOR UPDATE`: one composite row survives and both increments are preserved.

On 2026-09-27 the runner passed against an isolated PostgreSQL 18 instance on localhost:55432, including the full pending-confirmation lifecycle and additional supplier/reason/quantity/cost guards. Concurrent export tests produced both valid outcomes across runs: remaining stock 2 when export 8 won, and remaining stock 5 when export 5 won. The migration was also independently applied twice with the SQL assertion file after each application. These database tests validate migration and PostgreSQL locking behavior; application service and HTTP tests are separate.

Rollback after a successful production application requires the verified database backup and matching previous application version. There is no destructive automatic down migration: receipt states and online stock cannot safely be collapsed once the new workflow has been used.
