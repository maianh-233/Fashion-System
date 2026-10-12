# Realtime and notifications

## Overview

The system has one authenticated realtime connection per browser session. Notifications are the first feature built on it; Chat and other features must reuse the same gateway and client instead of opening their own sockets.

```text
GoodsReceiptService / GoodsIssueService
        | NotificationIntent (inside the business transaction)
        v
AfterCommitNotificationListener   (AFTER_COMMIT: resolve recipients)
        v
NotificationPublisher -> Kafka topic fashion.notifications
        v
NotificationConsumer -> NotificationPersistenceService (idempotent insert)
        v
RealtimePublisher -> STOMP /user/queue/events -> RealtimeProvider (browser)
```

Packages:

- `realtime` – identity, envelope, STOMP gateway and publisher. It knows nothing about notifications or Kafka.
- `notification` – intents, recipient resolution, Kafka transport, persistence, history API.
- `security.AccessTokenAuthenticationService` – JWT validation shared by the HTTP filter and the STOMP interceptor.

## Identity

`RealtimeIdentity(accountType, accountId)` addresses a recipient. `accountType` is `USER` (employee) or `CUSTOMER`. Its canonical principal is `<accountType>:<accountId>`, for example `USER:3f2a…`.

`AuthenticatedUser` and `AuthenticatedCustomer` implement `AuthenticatedAccount`, so `Principal.getName()` returns the canonical principal on both HTTP and STOMP. Employee and customer IDs can never collide in routing or in notification history.

## Envelope

Every realtime message uses the same feature-neutral envelope (`RealtimeEvent`):

```json
{
  "version": 1,
  "eventId": "uuid",
  "eventType": "NOTIFICATION_CREATED",
  "channel": "NOTIFICATION",
  "recipientType": "USER",
  "recipientId": "uuid",
  "payload": {},
  "timestamp": "2026-10-08T10:00:00Z"
}
```

`channel` identifies the feature and `eventType` the event within it. The browser dispatches on these two fields only.

## Backend gateway

- Native WebSocket endpoint `/ws` (no SockJS). Allowed origins come from `cors.allowed-origins`. Servlet security permits only the handshake path.
- `RealtimeStompInterceptor`:
  - `CONNECT` requires `Authorization: Bearer <access token>`, validated by `AccessTokenAuthenticationService` (revoked, expired, wrong-account, inactive, locked or deleted accounts are rejected);
  - `SUBSCRIBE` is allowed only to `/user/queue/events`;
  - client `SEND` is rejected.
- A live connection is not re-validated when its token expires. Logging out closes the browser socket; the next connect needs a valid token.

Features publish through `RealtimePublisher` only:

```java
realtimePublisher.sendToIdentity(identity, RealtimeEvent.create("MESSAGE_CREATED", "CHAT", identity, payload));
realtimePublisher.sendToUser(userId, event);       // USER shortcut
realtimePublisher.sendToUsers(userIds, event);     // deduplicated
```

Spring delivers to every open session (tab) of the principal. Delivery is best effort: a recipient that is offline misses the push and recovers it from persisted history.

## Browser client

`src/realtime/realtimeClient.js` owns one STOMP client. `RealtimeProvider` (mounted inside `AdminAuthProvider` in `App.jsx`) connects with the in-memory admin access token and follows the `lunaria:admin-session` event:

- a new token replaces the connection; logout disconnects and cancels any pending reconnect;
- reconnect uses exponential backoff (1 s doubling, capped at 30 s) and resets after success;
- if the broker rejects `CONNECT` (usually an access token that expired while offline), the client refreshes the token through `refreshAccessToken()` before retrying; a failed refresh clears the session;
- after a successful reconnect, `reconnectSequence` increments so features can refetch.

Features subscribe through the hook:

```jsx
const { subscribe, reconnectSequence } = useRealtime();
useEffect(() => subscribe({ channel: "CHAT", eventType: "MESSAGE_CREATED" }, handleMessage), [subscribe]);
```

In development, Vite proxies `/api` and `/ws` to `BACKEND_URL`. When the frontend is served from another origin, set `VITE_API_BASE_URL` (the socket URL is derived from it) or `VITE_WS_URL` explicitly.

## Notification flow

### Business intent

Warehouse services only call `NotificationIntentPublisher.publish(new NotificationIntent(...))`. The intent is a Spring application event; `AfterCommitNotificationListener` handles it with `@TransactionalEventListener(AFTER_COMMIT)`, so a rolled-back business transaction produces nothing, and any failure after commit is logged and never affects the business result.

| Workflow | Action | Recipients | Type |
| --- | --- | --- | --- |
| Goods receipt | submit | users with `IMPORT_RECEIPT_CONFIRM` covering the receipt store, excluding the actor | `PURCHASE_RECEIPT_SUBMITTED` |
| Goods receipt | confirm | `receivedBy`, unless they confirmed it themselves | `PURCHASE_RECEIPT_APPROVED` |
| Goods issue | submit | users with `EXPORT_RECEIPT_CONFIRM` covering the issue store, excluding the actor | `STOCK_EXPORT_SUBMITTED` |
| Goods issue | confirm | `issuedBy`, unless they confirmed it themselves | `STOCK_EXPORT_APPROVED` |

Action URLs point to `/admin/imports` and `/admin/exports`. Other approvers keep their historical "pending" notification after someone confirms; opening it shows the current state.

### Recipient resolution

`PermissionRecipientResolver.resolveUsers(permissionCode, storeId, excludedUserId)` runs one PostgreSQL query (`PermissionRecipientRepository`) that mirrors `EffectivePermissionQueryService` and `StoreAccessService`:

- user is active, not locked, not deleted;
- only permissions of active modules count;
- role grants merge at their widest scope; a direct `DENY` removes the permission; a direct `ALLOW` replaces the role scope;
- store staff (exactly one active store assignment) qualify when the assignment is the target store and the scope covers `STORE`;
- global staff (no active assignment) qualify only with scope `ALL`;
- staff assigned to several stores are excluded (their scope is ambiguous);
- the actor is excluded.

No role names or `managerId` are used, so administrators control recipients purely through permissions.

### Kafka transport

`NotificationPublisher` hides the transport. With `notification.kafka.enabled=true`, `KafkaNotificationPublisher` sends a `NotificationEvent` (keyed by `eventId`) to `notification.kafka.topic`; send failures are logged asynchronously. With the flag off (the default), `NoopNotificationPublisher` logs a warning and **no notification is stored**.

`NotificationConsumer` uses manual acknowledgement and the same retry + dead-letter handler as the audit pipeline (`KafkaClientConfigSupport`). Invalid events fail validation and end in `<topic>-dlt` after the configured retries.

### Persistence and idempotency

Each event/recipient pair is inserted in its own transaction. The unique index `uk_notifications_event_recipient (event_id, recipient_type, recipient_id)` makes redelivery safe: an existing row is skipped and **not** pushed again. Only newly inserted rows are published as `NOTIFICATION_CREATED`; a realtime failure is logged and does not undo the insert.

The legacy `notifications` table is reused (migration `V27__realtime_notifications.sql` adds `event_id`, `actor_user_id`, `type`, `action_url`, `metadata`). `created_at` is stored in UTC. Legacy `status`, `scheduled_at` and `sent_at` columns are not exposed and carry no read/unread meaning.

### History API

`GET /api/notifications?page=0&size=20` returns the caller's own notifications, newest first. The identity comes from the authenticated principal; there is no user ID parameter. `size` is capped at 100.

### Frontend feature

`NotificationProvider` loads history page by page, subscribes to `NOTIFICATION / NOTIFICATION_CREATED`, prepends new items, shows a toast and refetches the first page after reconnect. Items are deduplicated by notification `id`. `NotificationBell` in `AdminHeader` shows the list, a temporary dot for live arrivals (not an unread counter), "load more" and router navigation to `actionUrl`.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `NOTIFICATION_KAFKA_ENABLED` | `false` | Enables producer and consumer; required for notifications to be stored |
| `NOTIFICATION_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker list |
| `NOTIFICATION_KAFKA_TOPIC` | `fashion.notifications` | Topic (DLT is `<topic>-dlt`) |
| `NOTIFICATION_KAFKA_CONSUMER_GROUP` | `fashion-notification-consumer` | Consumer group |
| `NOTIFICATION_KAFKA_RETRY_ATTEMPTS` | `2` | Retries before dead-lettering |
| `NOTIFICATION_KAFKA_RETRY_BACKOFF_MS` | `1000` | Delay between retries |

Local Kafka can be started with `docker compose -f docker-compose.audit.yml up -d audit-kafka`; it auto-creates topics.

Migrations are applied manually: run `V27__realtime_notifications.sql` with `psql` before starting a build that includes this feature (`ddl-auto=validate` will otherwise fail on the missing columns).

## Limitations

- Single instance only. The simple in-memory STOMP broker cannot reach sockets held by another backend instance. Scaling out requires a `RealtimePublisher` implementation backed by Redis Pub/Sub or a STOMP broker relay; features do not change.
- No read/unread state, deletion, presence, delivery receipts or mobile push.
- Only the admin frontend opens a socket today. Customers are supported by the backend identity model; the customer site needs its own provider mount when a customer feature needs realtime.

## Adding a realtime feature (example: Chat)

1. Persist the feature's data in its own tables and service.
2. Publish envelopes on a new channel through `RealtimePublisher`, addressing `RealtimeIdentity.user(...)` or `RealtimeIdentity.customer(...)`.
3. In the browser, `subscribe({ channel: "CHAT" }, handler)` via `useRealtime()` and refetch on `reconnectSequence` changes.
4. If the client must send messages over the socket, extend the `RealtimeStompInterceptor` allowlist deliberately (for example `/app/chat.send`) and authorize each message server-side.

Do not add another WebSocket endpoint, session registry or browser socket.
