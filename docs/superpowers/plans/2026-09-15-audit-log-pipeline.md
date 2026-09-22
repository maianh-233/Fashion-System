 # Audit Log Pipeline Implementation Plan
 
 > For agentic workers: REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.
 
 **Goal:** Kafka-first asynchronous audit pipeline, Redis fallback/cache/rate-limit, unified audit API, and real data in /admin/logs.
 
 **Architecture:** Auth, business and request-filter events share an immutable AuditEvent. Kafka publishes without awaiting acknowledgement; Redis Stream is bounded fallback. Consumers persist idempotently into system_audit_logs; migration backfills audit_logs and auth_audit_logs. React reads the paginated API.
 
 **Tech Stack:** Spring Boot 4.1, Java 21, Spring Kafka, Spring Data Redis, PostgreSQL/Flyway, JPA, React/Vite, Vitest.
 
 **Spec:** docs/superpowers/specs/2026-09-15-audit-log-pipeline-design.md
 
 ## Global Constraints
 - Kafka/Redis failures never fail business requests.
 - Never log password, token, request body, password hash, OTP, raw email/phone or credentials.
 - Consumers are idempotent by event_id and ack only after persistence commit.
 - Keep old audit tables and backfill them into the unified source.
 - Keep LOG_VIEW authorization.
 - Every production change follows test RED, minimal implementation GREEN, then refactor.
 
 ### Task 1: Event contracts (RED)
 Files: create audit/AuditEventContractTest.java and audit/AuditEventSanitizerTest.java.
 - [ ] Test category/action/outcome/timestamp and removal of secret/body/PII keys.
 - [ ] Run .\mvnw.cmd -q -Dtest=AuditEventContractTest,AuditEventSanitizerTest test; confirm expected missing-type failure.
 - [ ] Commit tests: test: define audit event security contracts.
 
 ### Task 2: Event model/configuration
 Files: create audit/AuditEvent.java, AuditCategory.java, AuditOutcome.java, AuditEventSanitizer.java, config/AuditPipelineProperties.java; modify application.yaml and .env.example.
 Interfaces: immutable event with UUID eventId, category, action, outcome, nullable identities, request metadata, safe metadata, Instant occurredAt; sanitizer returns bounded immutable map.
 - [ ] Implement minimum model to turn Task 1 GREEN.
 - [ ] Add environment-backed Kafka topic/group, Redis stream/group, enabled flags, payload/retention bounds and defaults that start without Kafka.
 - [ ] Run focused tests and .\mvnw.cmd -q -DskipTests compile.
 - [ ] Commit: feat: add sanitized audit event model.
 
 ### Task 3: Unified persistence
 Files: create V16__system_audit_logs.sql, entity/SystemAuditLog.java, repository/SystemAuditLogRepository.java and its test.
 Interfaces: unique event_id, category/action/outcome, identities, username, request method/path/status/duration, IP/User-Agent, JSON metadata, created_at; paged filters; append-only trigger/indexes.
 - [ ] Write JSON round-trip and duplicate-event tests; run focused test and confirm RED.
 - [ ] Add migration and backfill existing audit_logs as BUSINESS and auth_audit_logs as AUTH without mutating source tables.
 - [ ] Implement save-if-absent behavior and run focused test GREEN.
 - [ ] Commit: feat: add unified system audit log persistence.
 
 ### Task 4: Kafka publisher and Redis fallback
 Files: modify pom.xml; create AuditEventPublisher.java, KafkaAuditEventPublisher.java, RedisAuditEventFallback.java, KafkaAuditConfig.java, RedisAuditConfig.java and focused tests.
 Interfaces: publish(AuditEvent) returns immediately; Kafka key is eventId; completion failure invokes bounded Redis Stream fallback; no transport exception escapes.
 - [ ] Write non-blocking and Kafka-failure fallback tests; run RED.
 - [ ] Add Spring Kafka dependency/config and JSON serialization.
 - [ ] Implement async send, callback fallback, bounded Stream, timeout and sanitized warnings.
 - [ ] Run focused tests/compile without requiring a live broker.
 - [ ] Commit: feat: publish audit events asynchronously.
 
 ### Task 5: Consumers
 Files: create AuditEventPersistenceService.java, KafkaAuditEventConsumer.java, RedisAuditEventConsumer.java, AuditEventMapper.java and focused tests.
 Interfaces: transactional persist returns inserted boolean; Kafka manual ack after commit; Redis consumer group acks after commit and recovers pending messages.
 - [ ] Test first insert, duplicate, failed transaction/no ack; run RED.
 - [ ] Implement persistence, validation, retry/backoff, manual ack, Redis pending recovery.
 - [ ] Run focused tests and disabled-transport context tests GREEN.
 - [ ] Commit: feat: persist audit events idempotently.
 
 ### Task 6: Auth/business routing
 Files: modify service/AuthAuditService.java and AuditLogService.java; create/modify their pipeline tests.
 - [ ] Test anonymous failed login, IP/User-Agent capture, business event and publisher-failure isolation; run RED.
 - [ ] Replace direct repository saves with event construction/publish while preserving public signatures and changed-field logic.
 - [ ] Bound forwarded IP/User-Agent and sanitize metadata.
 - [ ] Run focused tests plus AuthServiceLoginLockTest and AuditLogJsonPersistenceTest.
 - [ ] Commit: feat: route auth and business audit through pipeline.
 
 ### Task 7: System capture and Redis rate limit
 Files: create SystemAuditRequestFilter.java, DistributedRateLimitService.java and tests; modify SecurityConfig.java and RateLimitFilter.java.
 Interfaces: SYSTEM event has method/path/status/duration/actor/IP/User-Agent; exclude /api/admin/audit-logs, static, health/actuator and OPTIONS; allow(key, limit, window) uses atomic Redis increment+expiry with local best effort.
 - [ ] Write filter/rate-limit tests and run RED.
 - [ ] Register filter after security context and publish in finally without waiting.
 - [ ] Implement Redis counter/expiry and preserve current limits/paths.
 - [ ] Run focused tests plus RedisFailureFallbackTest.
 - [ ] Commit: feat: capture system requests and distribute rate limits.
 
 ### Task 8: Unified API
 Files: create SystemAuditLogDto.java and SystemAuditLogQueryService.java; modify AuditLogController.java and repository; create query/security tests.
 Interface: GET /api/admin/audit-logs returns Page with id/category/level/action/detail/actor/request/entity/IP/User-Agent/createdAt; supports category/action/entity/actor/time/page/size/sort; short Redis cache-aside.
 - [ ] Test backfilled/new rows, filters, pagination, invalid values and LOG_VIEW; run RED.
 - [ ] Implement query, safe level/detail mapping and normalized cache key; cache failure falls through to PostgreSQL.
 - [ ] Run focused and existing audit/security tests.
 - [ ] Commit: feat: expose unified audit log API.
 
 ### Task 9: Real /admin/logs UI
 Files: create hooks/adminLogsApi.js, loManagementLogic.js and test; modify LoManagement.jsx.
 Interfaces: fetchAdminLogs uses existing admin session helper; normalizeAdminLogsPage handles Spring Page fields.
 - [ ] Test query serialization, normalization, loading/error/empty and AUTH/SYSTEM/BUSINESS rendering; run npm test -- --run src/pages/admin/loManagementLogic.test.js and confirm RED.
 - [ ] Implement API helper, server pagination/filter/refresh/loading/error/empty states and remove hard-coded seeds.
 - [ ] Run focused Vitest and npm run build from frontend/react-app.
 - [ ] Commit: feat: render real audit logs in admin UI.
 
 ### Task 10: Operations and verification
 Files: update .env.example; create backend/docs/audit/AUDIT_LOG_PIPELINE.md and AuditLogPipelineContextTest.java.
 - [ ] Test startup with Kafka/Redis disabled; fix eager connections and turn GREEN.
 - [ ] Document variables, topic/group/stream, fallback, privacy and inspection steps without credentials.
 - [ ] Run .\mvnw.cmd -q test; record exact result.
 - [ ] Run npm test -- --run and npm run build in frontend/react-app.
 - [ ] Run git diff --check and verify unrelated worktree changes are untouched.
 - [ ] Commit: docs: document audit log operations.
 
 ## Execution Notes
 Create worktree isolation before implementation because the current worktree has unrelated changes. If brokers are unavailable, report unit/context verification separately and never claim live delivery.
 
