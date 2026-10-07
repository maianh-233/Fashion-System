# Shared Realtime Infrastructure and Notification System

## Goal and Scope

Build one authenticated realtime infrastructure for Fashion System and use it for persistent in-app notifications. Future Chat, order, approval, and inventory features must reuse this infrastructure. This phase does not implement Chat, presence, read/unread state, delivery receipts, deletion, or mobile push.

## Existing System Findings

- No WebSocket, STOMP, SSE, Socket.IO, or browser realtime client exists.
- Spring Kafka is already used by the audit subsystem with JSON serialization, retry, DLT, and an audit outbox.
- Redis currently serves read caching only.
- HTTP authentication is stateless JWT and distinguishes internal `USER` accounts from `CUSTOMER` accounts.
- Legacy Notification and Chat entities exist, but no notification service/API/realtime delivery exists. Chat entities remain untouched.
- Goods Receipt and Goods Issue implement submit and confirm flows. Submit has no assigned approver field.
- Authorization merges role grants and user overrides, with user `DENY` taking precedence. Store access is the intersection of identity scope and effective permission scope.

## Architecture

```text
Goods Receipt / Goods Issue
        |
        | NotificationIntent inside business transaction
        v
Transactional listener AFTER_COMMIT
        |
        v
NotificationPublisher -> Kafka fashion.notifications
        |
        v
NotificationConsumer
        |
        +-- persist Notification
        |
        +-- RealtimePublisher
                 |
                 v
        STOMP /user/queue/events
                 |
                 v
          shared RealtimeProvider
```

Business modules know neither Kafka topics nor WebSocket details. Notification is a feature using realtime infrastructure; realtime does not depend on Kafka or Notification.

## Shared Realtime Infrastructure

Use Spring WebSocket with STOMP and one endpoint. Spring manages user sessions and multiple simultaneous tabs; no custom session registry is introduced.

`RealtimeIdentity` contains:

```text
accountType: USER | CUSTOMER
accountId: UUID
canonicalPrincipal: <accountType>:<accountId>
```

Examples are `USER:3f...` and `CUSTOMER:7a...`. The canonical principal is the only key used by Spring user destinations and `convertAndSendToUser`.

`RealtimeEvent` is transport-generic:

```json
{
  "version": 1,
  "eventId": "uuid",
  "eventType": "NOTIFICATION_CREATED",
  "channel": "NOTIFICATION",
  "recipientType": "USER",
  "recipientId": "uuid",
  "payload": {},
  "timestamp": "instant"
}
```

The recipient type comes from the actual identity and is never hardcoded.

`RealtimePublisher` exposes `sendToUser`, `sendToUsers`, and an internal identity-based operation that supports both USER and CUSTOMER recipients. Feature modules never access STOMP, destinations, or sessions directly.

## Authentication and Connection Security

Extract token authentication from the servlet filter into a shared `AccessTokenAuthenticationService`. The existing HTTP filter and a STOMP channel interceptor both reuse it.

The STOMP interceptor:

- validates the Bearer token on `CONNECT`;
- assigns an authenticated principal whose `Principal.getName()` is the canonical identity;
- rejects unauthenticated connections;
- permits subscription only to `/user/queue/events`;
- rejects attempts to subscribe to another user or arbitrary destinations.

The browser never supplies a trusted user ID. HTTP authorization behavior and principal record types remain compatible with existing controllers.

## Connection Lifecycle

- One app-level `RealtimeProvider` owns one connection for the active session.
- Multiple tabs create separate sessions with the same principal; Spring delivers to all of them.
- Logout and account switching deactivate the socket and clear listeners.
- A refreshed access token causes reconnect with the new token; a live connection is not mutated.
- Reconnect uses bounded exponential backoff and resets after success.
- After reconnect, Notification refetches the first history page and deduplicates by notification ID.
- Realtime provides best-effort live delivery only; database history repairs missed events.

## Notification Persistence

Reuse the existing `notifications` table and entity. Add:

```text
event_id
actor_type nullable
actor_id nullable
type
action_url nullable
metadata jsonb nullable
```

Reuse existing fields:

```text
recipient_type -> USER | CUSTOMER
recipient_id
title
content        -> message
reference_type -> entity type
reference_id   -> entity id
created_at
```

Add a unique constraint on `(event_id, recipient_type, recipient_id)`. `notification.id` identifies history/UI records; `event_id` identifies the business notification event. Existing legacy status/scheduling columns are not exposed as read/unread or delivery semantics.

## Notification Kafka Flow

`NotificationPublisher` hides Kafka topic and serialization details. The Kafka transport reuses the existing broker dependency, JSON conventions, retry, and DLT approach while extracting shared client configuration instead of duplicating it.

Business services publish a local notification intent within their transaction. An `AFTER_COMMIT` listener resolves recipients and calls `NotificationPublisher`. Kafka failure is logged asynchronously and cannot roll back the completed business action.

The consumer validates the event, deduplicates recipients, persists notification rows, and only then publishes a `NOTIFICATION_CREATED` realtime event. A duplicate `(eventId, recipientType, recipientId)` creates no second row or push. Realtime failure is caught and logged separately and cannot roll back persistence.

## Permission Recipient Resolution

`PermissionRecipientResolver` is reusable for `permission + store + excluded actor`. It resolves recipients in a bounded query without per-user authorization calls.

For receipt submission it requires:

- active, unlocked, non-deleted internal user;
- effective `IMPORT_RECEIPT_CONFIRM` or `EXPORT_RECEIPT_CONFIRM` permission;
- role grants merged at their widest scope;
- user `DENY` overriding role grants and user `ALLOW` overriding role scope;
- store staff with exactly one active assignment at the receipt store and permission covering `STORE`; or
- global staff with no active store assignment and permission scope `ALL`;
- actor excluded and results deduplicated.

No role name or `managerId` is used.

## Business Integration

Receipt submit sends one event to all resolved approvers. Receipt confirm sends to `receivedBy`; issue confirm sends to `issuedBy`. Self-notification is skipped. Existing status transitions and authorization remain unchanged. No reject behavior is added.

Action URLs point to the existing admin import/export areas. Other approvers may retain stale historical notifications after one person confirms; opening the action loads the current backend state.

## Notification API

`GET /api/notifications?page=0&size=20` derives `RealtimeIdentity` from the authenticated principal, accepts no arbitrary user ID, sorts by `createdAt DESC`, and enforces a maximum page size. USER and CUSTOMER namespaces cannot overlap or access each other's records.

## Frontend

The shared realtime client parses the generic envelope and dispatches by `channel` and `eventType`. Notification registers the only handler in this phase.

The Notification provider:

- loads paginated history;
- handles `NOTIFICATION_CREATED`;
- prepends newest items and shows a toast;
- deduplicates realtime, reconnect, and refetch results by `notification.id`;
- loads more pages without fetching all history.

`AdminHeader` replaces hardcoded notification data with a NotificationBell/popover. There is no persisted unread count; a temporary dot may signal a newly received live event. Clicking a valid action URL navigates through the router.

## Redis and Multi-Instance Limitation

Redis Pub/Sub is not added in this single-instance phase. `RealtimePublisher` is the replacement boundary for a future Redis-backed fanout implementation. Notification, Chat, and other features must not depend on the local STOMP implementation.

## Testing and Verification

Tests cover canonical USER/CUSTOMER identity, JWT CONNECT authentication, SUBSCRIBE allowlist, cross-user isolation, multiple-session routing, permission/store recipient resolution, actor exclusion, Kafka failure isolation, consumer idempotency, realtime failure isolation, API ownership/pagination/sorting, receipt submit/confirm integrations, frontend deduplication, reconnect/refetch, and logout cleanup.

Auth regression covers employee and customer login, refresh, logout, HTTP JWT authentication, and STOMP authentication. Verification includes backend compile/tests and frontend lint/build.

## Future Chat Integration

```text
ChatService
    | persist message
    v
RealtimePublisher
    v
same STOMP gateway and RealtimeProvider
    v
recipient USER or CUSTOMER
```

Chat will define its own persistence and events, publish generic realtime envelopes on channel `CHAT`, and reuse the same authenticated connection. It must not add another WebSocket configuration, session registry, or feature-owned socket client.

