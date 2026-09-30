# Store warehouse implementation plan

Goal: secure per-store inventory and auditable import/export completion, synchronized with the React UI.

Architecture: reuse UUID entities, PostgreSQL, Spring transactional services, the existing JWT principal and effective RBAC. Keep legacy available_quantity as the physical offline column; add online_quantity, derive total. Retain reserved/damaged legacy data. Never mutate Product or ProductVariant stock.

The user's detailed business specification is authoritative and explicitly authorizes implementation without individual design approvals.

- [x] Extend inventory balance and movement snapshots; serialize mutations with existing upsert + pessimistic row lock, check integer overflow and nonnegative buckets.
- [x] Centralize action permission + employee store scope. ALL grants allow global employees across active stores; STORE grants require an assigned store; narrower grants do not authorize warehouse operations.
- [x] Refactor both receipt workflows and safe request DTOs. Lock the parent for every edit/transition. Generate codes on server. Complete only CONFIRMED; sorted variant locking; atomic ledger + inventory + final audit.
- [x] Add V22 SQL migration without restoring deleted historical files. Map APPROVED to COMPLETED without replaying stock; preserve legacy metadata. Add FK/checks/indexes and permission catalog migration.
- [x] Replace warehouse frontend with store entry, per-store inventory, import/export forms and history; permission/status actions, product/variant lookup, bucket quantity validation.
- [x] Test scope, permission, forbidden payloads, workflow, double completion, transfer conservation, insufficient stock, concurrent operations and migration. Run essential backend suites and frontend build/tests; full regression omitted per user quota instruction.
- [x] Review security, consistency and record actual verification and deployment limitations.

Review focus: duplicate variant lines must use accumulated stock; reversed receipt line ordering must not deadlock; unknown legacy statuses must not replay stock; global identity must not bypass narrow RBAC; stale UI requests must not display another store's data.
