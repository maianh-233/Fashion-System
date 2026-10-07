# Shared Realtime and Notification System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver one authenticated STOMP connection shared by present and future realtime features, with persistent Kafka-driven notifications for warehouse submit/confirm workflows.

**Architecture:** HTTP and STOMP reuse one JWT authentication service and namespace principals through `RealtimeIdentity`. Business transactions emit notification intents; after commit, Kafka transports notification events to an idempotent consumer that persists history and calls the transport-independent `RealtimePublisher`. A single React provider owns the socket and dispatches generic envelopes to the notification feature.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Security, Spring WebSocket/STOMP, Spring Kafka, PostgreSQL/JPA, React 19, `@stomp/stompjs`, Node test runner.

**Spec:** `docs/superpowers/specs/2026-10-08-realtime-notification-system-design.md`

## Global Constraints

- Do not implement Chat, presence, read/unread, delete, receipts, or push notifications.
- Core identity supports both `USER:<uuid>` and `CUSTOMER:<uuid>` from the first release.
- Only `/user/queue/events` may be subscribed to in this phase.
- Business success must not depend on Kafka or realtime delivery success.
- Notification history is persistent; WebSocket delivery is best effort.
- Do not add a custom session registry or a second frontend socket.
- Redis remains unused for realtime fanout in the single-instance phase.
- Work sequentially in the current session; do not use subagents.

## Review Focus

- A revoked or wrong-account JWT on STOMP CONNECT must fail without creating a principal; covered in Task 2.
- A client subscribing to any destination other than `/user/queue/events` must be rejected; covered in Task 2.
- User-level DENY and ambiguous multi-store assignment must exclude an apparent approver; covered in Task 5.
- A duplicate Kafka event after partial/retry processing must create and push no duplicate recipient row; covered in Task 6.
- Logout or account switch during reconnect backoff must cancel reconnect and leave no old-account listener; covered in Task 3.

---

### Task 1: Shared Authenticated Identity and JWT Authentication

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/security/AuthenticatedAccount.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/security/AccessTokenAuthenticationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/security/AuthenticatedUser.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/security/AuthenticatedCustomer.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/JwtAuthenticationFilter.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/security/AccessTokenAuthenticationServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/config/JwtSecurityIntegrationTest.java`

**Interfaces:**
- Produces: `AuthenticatedAccount.realtimeIdentity()`, canonical `Principal.getName()`, and `AccessTokenAuthenticationService.authenticate(String): Optional<Authentication>`.

- [ ] Write failing tests for `USER:<id>` and `CUSTOMER:<id>` names, valid employee/customer authentication, revoked/expired/invalid tokens, inactive/locked/deleted users, and current RBAC authorities.
- [ ] Run `mvn -q -Dtest=AccessTokenAuthenticationServiceTest,JwtSecurityIntegrationTest test` and confirm failures identify the missing shared service/interface.
- [ ] Implement `AuthenticatedAccount` and update both principal records without changing their existing ID/username accessors.
- [ ] Extract the current filter authentication logic into `AccessTokenAuthenticationService`; make `JwtAuthenticationFilter` delegate and set/clear the security context exactly as before.
- [ ] Run the focused tests and existing auth controller/service tests; expect PASS.
- [ ] Commit with `feat: share jwt authentication across transports`.

### Task 2: Generic STOMP Realtime Infrastructure

**Files:**
- Modify: `backend/fashion-system/pom.xml`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/SecurityConfig.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/RealtimeAccountType.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/RealtimeIdentity.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/RealtimeEvent.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/RealtimePublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/StompRealtimePublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/RealtimeStompInterceptor.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/realtime/RealtimeWebSocketConfig.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/realtime/RealtimeIdentityTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/realtime/RealtimeStompInterceptorTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/realtime/StompRealtimePublisherTest.java`

**Interfaces:**
- Consumes: `AccessTokenAuthenticationService.authenticate(String)` from Task 1.
- Produces: `RealtimeIdentity.of(accountType, accountId)`, `RealtimeEvent(version, eventId, eventType, channel, recipientType, recipientId, payload, timestamp)`, `RealtimePublisher.sendToIdentity`, `sendToUser`, and `sendToUsers`.

- [ ] Write failing tests for canonical identity, actual recipient type in envelopes, Bearer authentication on CONNECT, invalid/revoked token rejection, exact subscription allowlist, SEND rejection, and canonical `convertAndSendToUser` routing.
- [ ] Run the three realtime tests and confirm they fail before implementation.
- [ ] Add Spring WebSocket dependency and implement the generic models/publisher without Notification-specific payload types.
- [ ] Configure native STOMP endpoint `/ws`, application prefix `/app`, user prefix `/user`, and the simple broker queue needed by `/user/queue/events`; permit only the HTTP handshake path in servlet security.
- [ ] Implement inbound CONNECT/SUBSCRIBE/SEND checks and assign the shared authenticated principal to the STOMP accessor.
- [ ] Run focused realtime and JWT security tests; expect PASS.
- [ ] Commit with `feat: add authenticated shared realtime gateway`.

### Task 3: Shared Frontend Realtime Client

**Files:**
- Modify: `frontend/react-app/package.json`
- Modify: `frontend/react-app/package-lock.json`
- Modify: `frontend/react-app/src/App.jsx`
- Create: `frontend/react-app/src/realtime/realtimeClient.js`
- Create: `frontend/react-app/src/realtime/reconnectPolicy.js`
- Create: `frontend/react-app/src/realtime/RealtimeContext.jsx`
- Test: `frontend/react-app/src/realtime/reconnectPolicy.test.js`
- Test: `frontend/react-app/src/realtime/realtimeClient.test.js`

**Interfaces:**
- Consumes: the in-memory admin access token and session-change events.
- Produces: `RealtimeProvider`, `useRealtime()`, `subscribe({channel,eventType}, handler)`, and a reconnect-success signal.

- [ ] Write failing Node tests for bounded exponential delays, reset after success, token replacement, one active client, disconnect cancellation, generic dispatch, and ignored invalid envelopes.
- [ ] Run `node --test src/realtime/*.test.js` and confirm failures.
- [ ] Add `@stomp/stompjs`; implement one client with STOMP Authorization CONNECT header, bounded backoff, subscriber cleanup, and no feature-specific behavior.
- [ ] Mount `RealtimeProvider` inside `AdminAuthProvider` so token changes reconnect and logout disconnects; guard React Strict Mode double effects.
- [ ] Run Node tests and `npm run lint`; expect PASS.
- [ ] Commit with `feat: add shared frontend realtime provider`.

### Task 4: Notification Persistence and Secure History API

**Files:**
- Create: `backend/fashion-system/src/main/resources/db/migration/V27__realtime_notifications.sql`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/Notification.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/NotificationRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationType.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationResponse.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationQueryService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationController.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/notification/NotificationPersistenceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/notification/NotificationControllerSecurityTest.java`

**Interfaces:**
- Consumes: `AuthenticatedAccount.realtimeIdentity()`.
- Produces: paginated `GET /api/notifications` scoped to the current USER or CUSTOMER identity.

- [ ] Write failing persistence tests for new columns and uniqueness of `(event_id, recipient_type, recipient_id)`.
- [ ] Write failing controller tests proving no arbitrary user parameter, cross-namespace isolation, `createdAt DESC`, maximum page size, and authenticated USER/CUSTOMER access.
- [ ] Run focused notification persistence/controller tests and confirm failures.
- [ ] Add the explicit PostgreSQL migration, update the legacy entity, repository query, response mapping, query service, and controller. Leave legacy delivery/read columns outside the API.
- [ ] Run focused tests; expect PASS.
- [ ] Commit with `feat: add persistent notification history api`.

### Task 5: Reusable Permission Recipient Resolver

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/PermissionRecipientRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/PermissionRecipientResolver.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/notification/PermissionRecipientResolverTest.java`

**Interfaces:**
- Produces: `Set<RealtimeIdentity> resolveUsers(String permissionCode, UUID storeId, UUID excludedUserId)`.

- [ ] Write failing database-backed tests for role scope merge, direct ALLOW, direct DENY, matching/nonmatching store assignment, global `ALL`, global non-ALL exclusion, inactive/locked/deleted users, multi-store exclusion, actor exclusion, and duplicate grants.
- [ ] Run `mvn -q -Dtest=PermissionRecipientResolverTest test` and confirm failure.
- [ ] Implement one bounded PostgreSQL CTE query with explicit scope ranking and map distinct IDs to USER identities; do not loop through users or hardcode roles.
- [ ] Run the focused test; expect PASS and assert a bounded query count where supported.
- [ ] Commit with `feat: resolve notification recipients by permission scope`.

### Task 6: Notification Kafka Producer, Consumer, and Idempotency

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/KafkaClientConfigSupport.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/KafkaAuditConfig.java`
- Modify: `backend/fashion-system/src/main/resources/application.yaml`
- Modify: `backend/fashion-system/.env.example`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationEvent.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationPublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/KafkaNotificationPublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NoopNotificationPublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationKafkaConfig.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationConsumer.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationPersistenceService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/notification/KafkaNotificationPublisherTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/notification/NotificationConsumerTest.java`

**Interfaces:**
- Consumes: recipient identities from Task 5 and `RealtimePublisher` from Task 2.
- Produces: validated multi-recipient Kafka events and persisted `NOTIFICATION_CREATED` realtime delivery.

- [ ] Write failing tests for recipient deduplication, empty/invalid event rejection, async producer failure logging, duplicate consumption, persistence-before-push, no push for existing rows, and caught realtime failure.
- [ ] Run focused Kafka/consumer tests and confirm failures.
- [ ] Extract shared Kafka property/error-handler construction from audit config without changing audit behavior; add notification-specific typed beans, topic/group/retry/DLT properties, and disabled fallback publisher.
- [ ] Implement producer mapping and non-blocking failure handling.
- [ ] Implement transactional idempotent persistence followed by separately guarded realtime publication with payload containing notification ID, type, title, message, action URL, entity reference, and created time.
- [ ] Run focused tests plus existing audit Kafka/outbox tests; expect PASS.
- [ ] Commit with `feat: add kafka notification pipeline`.

### Task 7: After-Commit Warehouse Notification Integration

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationIntent.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/NotificationIntentPublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/notification/AfterCommitNotificationListener.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsReceiptService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsIssueService.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/GoodsReceiptScopeTest.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/GoodsIssueScopeTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/notification/AfterCommitNotificationListenerTest.java`

**Interfaces:**
- Consumes: `PermissionRecipientResolver` and `NotificationPublisher`.
- Produces: submit notifications for permission recipients and confirm notifications for the creator, only after commit.

- [ ] Write failing tests for import/export submit intent, import/export confirm intent, correct permission/store/action URL, actor exclusion, creator self-notification suppression, and no intent on failed transaction/status transition.
- [ ] Write failing listener tests proving permission resolution/publish failure is logged and cannot alter the completed business transaction.
- [ ] Run focused warehouse/listener tests and confirm failures.
- [ ] Implement generic intent publishing and `@TransactionalEventListener(AFTER_COMMIT)` handling; inject only `NotificationIntentPublisher` into warehouse services.
- [ ] Run focused tests; expect PASS.
- [ ] Commit with `feat: notify warehouse submit and confirm workflows`.

### Task 8: Notification Frontend and Header Bell

**Files:**
- Create: `frontend/react-app/src/api/notificationsApi.js`
- Create: `frontend/react-app/src/features/notifications/notificationState.js`
- Create: `frontend/react-app/src/features/notifications/NotificationContext.jsx`
- Create: `frontend/react-app/src/features/notifications/NotificationBell.jsx`
- Create: `frontend/react-app/src/features/notifications/NotificationBell.css`
- Modify: `frontend/react-app/src/App.jsx`
- Modify: `frontend/react-app/src/components/admin/AdminHeader.jsx`
- Test: `frontend/react-app/src/features/notifications/notificationState.test.js`

**Interfaces:**
- Consumes: `useRealtime()`, reconnect-success signal, and `GET /api/notifications`.
- Produces: paginated history, realtime prepend/toast, notification deduplication, and action navigation.

- [ ] Write failing pure-state tests for newest-first merge, notification-ID dedup across realtime/refetch/pages, stable pagination, and invalid payload rejection.
- [ ] Run `node --test src/features/notifications/*.test.js` and confirm failure.
- [ ] Implement the authenticated API wrapper and Notification provider; subscribe to `channel=NOTIFICATION,eventType=NOTIFICATION_CREATED`, refetch first page after reconnect, and use the existing toast system.
- [ ] Replace AdminHeader hardcoded list/count with NotificationBell, temporary live dot, loading/empty/error states, load-more pagination, relative timestamps, and router action navigation.
- [ ] Run frontend Node tests, lint, and production build; expect PASS.
- [ ] Commit with `feat: add realtime notification bell`.

### Task 9: Regression, Documentation, and Final Verification

**Files:**
- Create: `docs/realtime-notification-architecture.md`
- Modify tests only if verification exposes a defect within this feature.

**Interfaces:**
- Consumes: all prior task outputs.
- Produces: operator/developer documentation and verified release evidence.

- [ ] Document the envelope, publisher API, canonical identity, STOMP authentication/allowlist, connection lifecycle, frontend dispatcher, Kafka flow, failure isolation, recipient resolver, Redis limitation, and Future Chat reuse examples.
- [ ] Run employee/customer login-refresh-logout and HTTP/STOMP security regression suites; expect PASS.
- [ ] Run all backend tests with `mvn test -q`; expect exit code 0.
- [ ] Run frontend `node --test`, `npm run lint`, and `npm run build`; expect exit code 0.
- [ ] Inspect changed files for Notification coupling in realtime, low-level socket/Kafka calls in business services, cross-user leaks, duplicate socket creation, N+1 queries, and unrelated Chat/read-state work; fix any finding and rerun affected checks.
- [ ] Record any environment-dependent Kafka/PostgreSQL integration limitation in the final report rather than claiming an unrun check.
- [ ] Commit with `docs: document realtime notification architecture`.

