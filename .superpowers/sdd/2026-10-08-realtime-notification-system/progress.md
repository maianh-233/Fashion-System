# SDD ledger — plan: docs/superpowers/plans/2026-10-08-realtime-notification-system.md

Execution: inline, sequential, no subagents (explicit user constraint).
Merge base: bfa6cc3
Setup Ruling: bundled SDD helper scripts require Bash, but this Windows host has no WSL distribution — maintain the same plan-scoped workspace, briefs, BASE values, test evidence, and completion lines with PowerShell/apply-patch — cost if wrong: only resumability metadata, not product behavior.
Pre-flight 1→2: `AccessTokenAuthenticationService.authenticate(String)` and canonical authenticated principal match Task 2 CONNECT consumption.
Pre-flight 1→4: `AuthenticatedAccount.realtimeIdentity()` matches Notification API identity scoping.
Pre-flight 2→3: `/ws`, `/user/queue/events`, and generic envelope match frontend client consumption.
Pre-flight 2+5→6: identity recipients and `RealtimePublisher` match Kafka consumer inputs.
Pre-flight 5+6→7: resolver and publisher contracts match after-commit integration.
Pre-flight 3+4+6→8: realtime subscription, history API, and payload notification ID match frontend deduplication.
Baseline Ruling: frontend lint had 17 errors unrelated to this feature; all were repaired before feature work, and both `npm run lint` and production build pass — cost if wrong: unrelated UI code could hide a regression, so changes are isolated in a baseline commit.
Baseline Ruling: full backend suite executed 462 tests with 0 assertion failures and 16 ApplicationContext errors because PostgreSQL placeholders (`DB_HOST`, `DB_PORT`, `DB_NAME`) are unavailable in this worktree — continue with focused unit tests and rerun integration tests when a disposable database is configured — cost if wrong: a database integration regression may remain undetected locally.
Plan Ruling: create `RealtimeIdentity` and account-type primitives in Task 1 instead of Task 2 because authenticated principals must expose canonical realtime identity — Task 2 will consume these shared types — cost if wrong: package ownership could need a mechanical move, with no protocol change.
Task 1 BASE=c3a1c59
Task 1 RED: `AccessTokenAuthenticationServiceTest` failed to compile because the shared service did not exist.
Task 1 GREEN: focused identity/authentication tests pass; Employee/Customer login and refresh unit regressions pass; backend clean compile passes.
Task 1 Complete: shared canonical principals and access-token authentication extracted for HTTP/STOMP reuse.
Task 2 BASE=84b884b
Task 2 RED: realtime tests failed because STOMP classes and shared gateway implementations did not exist.
Task 2 GREEN: identity/envelope, canonical user routing, CONNECT authentication, exact SUBSCRIBE allowlist, and client SEND rejection tests pass; backend clean compile passes.
Task 2 Ruling: use the native WebSocket endpoint (no SockJS) and a custom inbound interceptor because browser credentials are carried in the STOMP CONNECT frame; servlet security permits only the handshake path — cost if wrong: clients without native WebSocket support would need a later SockJS fallback.
Task 2 Complete: one generic authenticated STOMP gateway and publisher now serve all realtime feature channels.
Task 3 BASE=af6290b
Task 3 RED: Node tests failed because the shared realtime client and reconnect policy did not exist.
Task 3 GREEN: 6 Node tests pass for singleton/token replacement, logout cancellation, generic dispatch, invalid-envelope rejection, reconnect signaling, and capped exponential backoff; frontend lint and production build pass.
Task 3 Complete: one app-level RealtimeProvider owns the admin socket and reconnects with each new in-memory access token.
Task 4 BASE=e830059
Task 4 RED: notification tests failed because event id, idempotency mapping, scoped repository query, response, service, and controller were absent.
Task 4 GREEN: focused tests pass for the unique event/recipient mapping, USER/CUSTOMER scope isolation, no arbitrary recipient parameter, created-desc ordering, and page-size cap; backend clean compile passes.
Task 4 Ruling: database-backed migration execution remains unavailable without a disposable PostgreSQL configuration; validate the explicit V27 DDL structurally and with unit tests now — cost if wrong: PostgreSQL-specific migration behavior must still be confirmed in integration/CI.
Task 4 Complete: legacy notifications table is extended and exposed through an authenticated identity-scoped paginated history API.
Task 5 BASE=23aee29
Task 5 RED: resolver test failed because no shared permission recipient repository/service existed.
Task 5 GREEN: focused tests pass for permission normalization, recipient deduplication, actor exclusion, USER identities, and query rules covering DENY/ALLOW precedence, widest role scope, global/store identity scope, active/locked/deleted filtering, exact Store matching, and multi-Store exclusion.
Task 5 Ruling: validate the single PostgreSQL CTE structurally because the disposable database is unavailable locally — cost if wrong: native SQL dialect/binding still needs CI/database confirmation.
Task 5 Complete: workflows can resolve approvers by permission + Store scope without role or manager assumptions.
Task 6 BASE=11a3129
Task 6 RED: Kafka tests failed because notification request/event, producer, persistence, consumer, and realtime bridge did not exist.
Task 6 GREEN: producer/consumer/idempotency tests pass for recipient deduplication, invalid requests, async Kafka failure isolation, persistence-before-push, duplicate suppression, realtime failure isolation, and acknowledgment; existing Kafka audit/outbox tests pass; backend clean compile passes.
Task 6 Ruling: notification Kafka is disabled by default and uses a no-op publisher; production enables it explicitly after topic/broker provisioning — cost if wrong: environments that forget the flag retain history/API code but do not create new notification rows.
Task 6 Complete: typed Kafka notification transport persists idempotently and pushes generic realtime events without coupling realtime to Kafka.
Task 7 BASE=65dfa2a
Task 7 RED: warehouse tests failed because intent types/publisher/listener and service injection did not exist.
Task 7 GREEN: receipt/issue tests pass for submit and approve intents, correct permission/store/action URL, actor exclusion, creator-only approval delivery, self-notification suppression, no intent on invalid transitions, AFTER_COMMIT phase, and contained resolver failure; transaction tests and backend clean compile pass.
Task 7 Complete: warehouse business services emit only local intents; recipient resolution, Kafka, and realtime remain outside business code and execute after commit.
Task 8 BASE=a93fee4
Task 8 RED: frontend notification state tests failed because no feature state/API/provider existed.
Task 8 GREEN: 9 notification/realtime Node tests pass for newest-first merge, notification-ID dedup across realtime/refetch/pages, invalid payload rejection, singleton/reconnect behavior; frontend lint and production build pass; focused backend notification tests pass after UTC timestamp normalization.
Task 8 Complete: the admin header uses persistent paginated history, realtime prepend/toast, a temporary live dot (not unread state), action navigation, and reconnect refetch.
Handoff: Codex session ended after Task 8; Claude Code resumed Task 9 on 2026-10-12.
Task 9 BASE=c654689
Task 9 Finding: Vite dev proxy forwarded only /api, so the default local socket URL ws://<vite>/ws never reached the backend — fixed by proxying /ws with ws:true; verified by running the e2e flow through the Vite port.
Task 9 Finding: reconnect after the 30-minute access token expired retried forever with the stale token — client now refreshes the token after a broker ERROR frame before retrying; 3 Node tests added; server ERROR-before-close behavior confirmed live.
Task 9 Finding: recipient CTE counted grants of inactive modules unlike RbacService — fixed; PostgreSQL-backed PermissionRecipientRepositoryIntegrationTest covers scope merge, DENY/ALLOW, store/global identity, multi-store, locked, actor, inactive module.
Task 9 Finding: notification.kafka.enabled=true failed application startup (notificationClock made audit's unqualified Clock injections ambiguous) — bean removed; NotificationKafkaContextTest added.
Task 9 Ruling: verification ran on a disposable clone (fashion_realtime_verify) of the local commerce_db with V22 and V27 applied; commerce_db itself was not modified. Local commerce_db lacks V22/V26 and has Hibernate-created customer tables, so V25/V26 do not apply cleanly — pre-existing drift, outside this feature.
Task 9 E2E: 16/16 live checks pass with Kafka enabled (direct and via Vite proxy): CONNECT/SUBSCRIBE rejection, multi-tab delivery, store + global approvers, actor exclusion, envelope identity, history ordering/isolation/page cap, approval to creator. Replaying a consumed Kafka record created no duplicate row (offset 3/3, rows unchanged).
Task 9 Regression: full backend suite on the clone — only failures are 3 pre-existing schema-drift errors (customers.username, audit_logs.changed_fields) reproduced identically on main.
