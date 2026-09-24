# Task 8 report

Implemented separate business and authentication history endpoints and UI views.

- `/api/admin/audit-logs` reads only `audit_logs`; its DTO exposes actor, request/job metadata, row count, changes, timestamps, and migration provenance. Filters: actorUserId, username, action, table, rowId, requestId, fromAt (inclusive), toAt (exclusive).
- `/api/admin/auth-audit-logs` reads only `auth_audit_logs`, restricted to LOGIN_SUCCESS, LOGIN_FAILED and LOGOUT, with actor/action/time filters. Both endpoints require LOG_VIEW and paginate/count in SQL with stable ordering and maximum page size 200.
- V21 copies the four legacy employee/subordinate actions using deterministic IDs and conflict-safe inserts. Source evidence is retained. `migratedFromAuthAudit=true`, source ID and description are preserved in new_data and provenance is exposed by the response. Missing row snapshots remain empty and row count remains unknown, avoiding fabricated history.
- Separate frontend API functions and “Thao tác dữ liệu” / “Đăng nhập / đăng xuất” views use one endpoint each. Switching views resets page/loading state; stale requests are ignored. Business details show request metadata and row changes. Existing session/product UI remains intact.

Verification: `git diff --check` completed with exit 0 (line-ending warnings only). No tests, backend compile, frontend build, browser verification, migration execution or database query execution were performed, as directed by the fast-path handoff. Runtime SQL and migration behavior remain unverified. Existing unrelated Task 9 changes were not staged.
