# Reliable Business Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ghi đúng một business audit bất biến cho mỗi request/job thay đổi dữ liệu, không mất event khi Kafka lỗi, đồng thời giữ `auth_audit_logs` chỉ cho đăng nhập/đăng xuất.

**Architecture:** Hibernate change capture gom mọi insert/update/delete trong transaction vào một request-scoped change-set; một Spring AOP boundary ghi event vào transactional outbox trước khi transaction nghiệp vụ commit. Worker riêng append event vào file JSONL hash-chain và gửi Kafka; consumer ghi `audit_logs` bằng transaction riêng, còn scheduler lúc 0 giờ đối soát file với DB và chỉ xóa file đã đầy đủ.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring MVC/Security/AOP/Scheduling, Spring Data JPA + Hibernate, PostgreSQL JSONB, Spring Kafka, Jackson, JUnit 5/Mockito, React/Vitest.

**Spec:** `docs/superpowers/specs/2026-09-24-reliable-business-audit-design.md`

## Global Constraints

- `auth_audit_logs` chỉ nhận `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`.
- Một transaction nghiệp vụ thành công sinh tối đa một business event; transaction rollback không sinh event.
- Audit actor chỉ lấy từ server security context; job nền dùng `SYSTEM` và `jobName`.
- API nghiệp vụ không chờ Kafka consumer và không dùng transaction ghi `audit_logs`.
- Outbox nằm trong cùng commit với thay đổi nghiệp vụ để không có crash gap.
- Password, hash, token, cookie, authorization, OTP, secret, API key và binary content luôn bị loại hoặc `[REDACTED]`.
- File ngày dùng timezone `Asia/Saigon`; chỉ xóa khi toàn bộ `eventId` đã có trong DB và hash chain hợp lệ.
- `audit_logs` append-only; Kafka/file retry idempotent theo `eventId`.
- Không ghi audit cho bảng audit/outbox/session/token/OTP và mutation hạ tầng.
- Giữ nguyên các thay đổi chưa commit hiện có; mỗi commit bên dưới chỉ stage đúng file của task.

## Review Focus

- Một entity update nhiều lần hoặc insert rồi delete trong cùng transaction phải được gộp đúng; Task 2 có test trạng thái đầu/cuối và no-op cuối cùng.
- Proxy lồng nhau và transaction `REQUIRES_NEW` không được tạo trùng hoặc nhập nhầm change-set; Task 3 có integration test hai transaction boundary.
- Worker crash giữa file append và cập nhật outbox có thể tạo dòng lặp; Task 4 và Task 6 kiểm tra deduplicate theo `eventId`.
- Nhiều instance không được ghi chung một daily file; Task 4 kiểm tra filename có `instanceId` và file lock.
- Ngày đổi theo `Asia/Saigon`, kể cả sát nửa đêm; Task 6 dùng fixed `Clock` để kiểm tra đúng file ngày hôm trước.

---

### Task 1: Event contract, outbox và schema business audit theo request

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditActorType.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditOperation.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditChange.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditEvent.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/AuditOutbox.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/AuditOutboxRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/AuditLog.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V20__reliable_business_audit.sql`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/repository/AuditOutboxPersistenceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/repository/AuditLogRequestEventPersistenceTest.java`

**Interfaces:**
- Consumes: PostgreSQL UUID/JSONB and the existing `AuditEvent.eventId()` idempotency contract.
- Produces: `AuditChange(table, rowId, operation, changedFields, oldValues, newValues)`; request-level `AuditEvent`; `AuditOutboxRepository.saveAndFlush`, `findAll`, `findByEventId(UUID)` and `lockPending(Instant now, Pageable page)`.

- [ ] **Step 1: Write failing persistence tests**

```java
@Test
void stores_one_outbox_payload_for_a_multi_row_event() {
    AuditEvent event = Fixtures.event(List.of(
            Fixtures.change("users", "u-1", AuditOperation.UPDATE),
            Fixtures.change("user_roles", "u-1:r-2", AuditOperation.INSERT)));
    AuditOutbox saved = repository.saveAndFlush(AuditOutbox.pending(event, objectMapper));
    entityManager.clear();
    AuditOutbox found = repository.findByEventId(event.eventId()).orElseThrow();
    assertThat(found.getPayload()).isEqualTo(objectMapper.valueToTree(event));
    assertThat(found.getFileAppendedAt()).isNull();
    assertThat(found.getKafkaPublishedAt()).isNull();
}

@Test
void stores_request_level_audit_with_row_count_and_changes() {
    AuditLog saved = repository.save(Fixtures.auditLogWithTwoChanges());
    entityManager.flush();
    entityManager.clear();
    AuditLog found = repository.findByEventId(saved.getEventId()).orElseThrow();
    assertThat(found.getRowCount()).isEqualTo(2);
    assertThat(found.getChanges()).hasSize(2);
    assertThat(found.getRequestId()).isNotBlank();
}
```

- [ ] **Step 2: Run the tests and verify schema/model failures**

Run: `mvn -q -Dtest=AuditOutboxPersistenceTest,AuditLogRequestEventPersistenceTest test`

Expected: FAIL because `AuditOutbox`, request-level fields and V20 schema do not exist.

- [ ] **Step 3: Add the migration and minimal domain types**

```java
public record AuditChange(String table, String rowId, AuditOperation operation,
        List<String> changedFields, JsonNode oldValues, JsonNode newValues) {}

public record AuditEvent(UUID eventId, int schemaVersion, AuditActorType actorType,
        UUID actorUserId, String username, String action, String requestId,
        String method, String path, String jobName, String ipAddress, String userAgent,
        int rowCount, List<AuditChange> changes, Instant occurredAt) {}
```

V20 must create `audit_outbox(event_id unique, payload jsonb, occurred_at, created_at, file_appended_at, kafka_published_at, attempt_count, next_attempt_at, last_error)` and add `actor_type`, `request_id`, `method`, `path`, `job_name`, `row_count`, `changes`, `occurred_at` to `audit_logs`. Make old single-entity columns nullable for migrated rows, retain the append-only trigger, and add GIN/index support for `changes`, actor/action/time and request ID.

- [ ] **Step 4: Run focused persistence tests**

Run: `mvn -q -Dtest=AuditOutboxPersistenceTest,AuditLogRequestEventPersistenceTest test`

Expected: PASS with two request-level changes round-tripping through JSONB.

- [ ] **Step 5: Commit the schema contract**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditActorType.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditOperation.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditChange.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditEvent.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/AuditOutbox.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/AuditLog.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/AuditOutboxRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V20__reliable_business_audit.sql backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/repository/AuditOutboxPersistenceTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/repository/AuditLogRequestEventPersistenceTest.java
git commit -m "feat(audit): add request event and durable outbox schema"
```

### Task 2: Transaction change collector, sanitizer và Hibernate capture

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditChangeKey.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditChangeCollector.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditCaptureScope.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/HibernateAuditInterceptor.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditEventSanitizer.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/HibernateAuditConfig.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/AuditChangeCollectorTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/AuditEventSanitizerTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/HibernateAuditInterceptorTest.java`

**Interfaces:**
- Consumes: entity callback values from Hibernate and `ObjectMapper`.
- Produces: `AuditCaptureScope.open()`, `AuditCaptureScope.current()`, `AuditChangeCollector.recordInsert/recordUpdate/recordDelete`, `List<AuditChange> finish()`.

- [ ] **Step 1: Write collector and sanitizer tests first**

```java
@Test
void merges_repeated_updates_to_first_old_and_last_new() {
    collector.recordUpdate("users", "u1", json("{\"name\":\"A\"}"), json("{\"name\":\"B\"}"));
    collector.recordUpdate("users", "u1", json("{\"name\":\"B\"}"), json("{\"name\":\"C\"}"));
    AuditChange change = collector.finish().getFirst();
    assertThat(change.oldValues().get("name").asText()).isEqualTo("A");
    assertThat(change.newValues().get("name").asText()).isEqualTo("C");
    assertThat(change.changedFields()).containsExactly("name");
}

@Test
void insert_then_delete_has_no_final_change() {
    collector.recordInsert("users", "u1", json("{\"name\":\"A\"}"));
    collector.recordDelete("users", "u1", json("{\"name\":\"A\"}"));
    assertThat(collector.finish()).isEmpty();
}

@Test
void redacts_secrets_recursively() {
    JsonNode result = sanitizer.sanitize(json("{\"profile\":{\"passwordHash\":\"x\",\"name\":\"An\"},\"token\":\"y\"}"));
    assertThat(result.at("/profile/passwordHash").asText()).isEqualTo("[REDACTED]");
    assertThat(result.get("token").asText()).isEqualTo("[REDACTED]");
}
```

- [ ] **Step 2: Verify the tests fail for missing capture behavior**

Run: `mvn -q -Dtest=AuditChangeCollectorTest,AuditEventSanitizerTest,HibernateAuditInterceptorTest test`

Expected: FAIL because collector/scope/interceptor do not exist and sanitizer does not recursively enforce the denylist.

- [ ] **Step 3: Implement capture with explicit exclusions**

Use a thread-local map keyed by the identity of the current `EntityManagerHolder` obtained from `TransactionSynchronizationManager.getResource(entityManagerFactory)`, plus a stack for restoration, and register the interceptor through `HibernatePropertiesCustomizer`. Spring suspends/restores that resource for `REQUIRES_NEW`, so nested `REQUIRED` calls share a collector while a suspended outer transaction and its inner transaction receive separate collectors. Resolve `@Table(name)` and identifier strings; build old/new JSON from Hibernate property arrays. Exclude these entity classes unconditionally: `AuditLog`, `AuthAuditLog`, `AuditOutbox`, `RevokedToken`, `UserToken`, `PasswordResetOtp` and `PaymentWebhookLog`.

```java
public final class AuditCaptureScope implements AutoCloseable {
    public static AuditCaptureScope open(AuditChangeCollector collector);
    public static Optional<AuditChangeCollector> current();
    @Override public void close();
}
```

Ensure `close()` restores the previous session-keyed collector and clears the thread-local map after the outer scope, including exceptions.

- [ ] **Step 4: Run focused unit tests**

Run: `mvn -q -Dtest=AuditChangeCollectorTest,AuditEventSanitizerTest,HibernateAuditInterceptorTest test`

Expected: PASS for insert/update/delete, repeated mutations, no-op final state, recursive redaction, composite IDs and exclusions.

- [ ] **Step 5: Commit capture infrastructure**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditChangeKey.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditChangeCollector.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditCaptureScope.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/HibernateAuditInterceptor.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditEventSanitizer.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/HibernateAuditConfig.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/AuditChangeCollectorTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/AuditEventSanitizerTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/HibernateAuditInterceptorTest.java
git commit -m "feat(audit): capture and sanitize transaction changes"
```

### Task 3: Business audit boundary và atomic outbox write

**Files:**
- Modify: `backend/fashion-system/pom.xml`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAudit.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAuditContext.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAuditContextFactory.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAuditAspect.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/AuditTransactionConfig.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/BusinessAuditAspectIntegrationTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/BusinessAuditContextFactoryTest.java`

**Interfaces:**
- Consumes: Task 2 collector and Task 1 `AuditOutboxRepository`.
- Produces: `@BusinessAudit("EMPLOYEE")` at service type/method; `BusinessAuditContextFactory.forCurrentRequest(String domain, String methodName)`; one outbox row in the current transaction.

- [ ] **Step 1: Write red tests for commit, rollback, nesting and system actor**

```java
@Test
void committed_multi_table_service_call_writes_exactly_one_outbox_event() {
    auditedFixtureService.changeUserAndRole();
    List<AuditOutbox> rows = outboxRepository.findAll();
    assertThat(rows).hasSize(1);
    AuditEvent event = decode(rows.getFirst());
    assertThat(event.rowCount()).isEqualTo(2);
    assertThat(event.action()).isEqualTo("EMPLOYEE_CHANGE_USER_AND_ROLE");
}

@Test
void rollback_writes_no_outbox_event() {
    assertThatThrownBy(auditedFixtureService::changeThenFail).isInstanceOf(IllegalStateException.class);
    assertThat(outboxRepository.findAll()).isEmpty();
}

@Test
void requires_new_child_is_a_separate_event() {
    auditedFixtureService.outerWithRequiresNewChild();
    assertThat(outboxRepository.findAll()).hasSize(2);
}
```

- [ ] **Step 2: Run integration tests and see the missing boundary fail**

Run: `mvn -q -Dtest=BusinessAuditAspectIntegrationTest,BusinessAuditContextFactoryTest test`

Expected: FAIL because `@BusinessAudit` and atomic outbox persistence are absent.

- [ ] **Step 3: Implement aspect inside the Spring transaction advice**

Add `spring-boot-starter-aop`. Set transaction advice order before the audit aspect, so execution is `transaction -> audit aspect -> service method -> flush -> outbox save -> commit`.

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessAudit { String value(); }

@Around("@within(marker) || @annotation(marker)")
public Object audit(ProceedingJoinPoint joinPoint, BusinessAudit marker) throws Throwable {
    AuditChangeCollector collector = new AuditChangeCollector(sanitizer);
    try (AuditCaptureScope ignored = AuditCaptureScope.open(collector)) {
        Object result = joinPoint.proceed();
        entityManager.flush();
        List<AuditChange> changes = collector.finish();
        if (!changes.isEmpty()) outboxRepository.save(AuditOutbox.pending(eventFactory.create(marker.value(), joinPoint, changes), objectMapper));
        return result;
    }
}
```

Nested calls that join the same transaction reuse the current collector and outer event; a true `REQUIRES_NEW` transaction opens its own collector. Reject use without an active transaction to prevent an event detached from business commit.

- [ ] **Step 4: Run transaction-boundary tests**

Run: `mvn -q -Dtest=BusinessAuditAspectIntegrationTest,BusinessAuditContextFactoryTest test`

Expected: PASS; committed changes and outbox are atomic, rollback is empty, actor comes from `AuthenticatedUser`, and an explicit job scope produces `SYSTEM` plus `jobName`.

- [ ] **Step 5: Commit the atomic boundary**

```bash
git add backend/fashion-system/pom.xml backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAudit.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAuditContext.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAuditContextFactory.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BusinessAuditAspect.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/AuditTransactionConfig.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/BusinessAuditAspectIntegrationTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/BusinessAuditContextFactoryTest.java
git commit -m "feat(audit): atomically enqueue one event per transaction"
```

### Task 4: Daily append-only JSONL spool với hash chain

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolProperties.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolLine.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolWriter.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolReader.java`
- Modify: `backend/fashion-system/src/main/resources/application.yaml`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolWriterTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolReaderTest.java`

**Interfaces:**
- Consumes: canonical `AuditEvent` JSON and injected `Clock`.
- Produces: `Path append(AuditEvent event)`, `AuditSpoolReadResult readAndVerify(LocalDate date)`, filename `business-audit-YYYY-MM-DD-<instanceId>.jsonl`.

- [ ] **Step 1: Write file behavior tests using `@TempDir`**

```java
@Test
void appends_canonical_lines_with_a_valid_hash_chain(@TempDir Path dir) {
    writer(dir, "node-a", fixedClock).append(event1);
    writer(dir, "node-a", fixedClock).append(event2);
    AuditSpoolReadResult result = reader(dir).readAndVerify(LocalDate.of(2026, 9, 24));
    assertThat(result.valid()).isTrue();
    assertThat(result.events()).extracting(AuditEvent::eventId)
            .containsExactly(event1.eventId(), event2.eventId());
}

@Test
void detects_modified_middle_line(@TempDir Path dir) {
    Path file = writeTwoEvents(dir);
    Files.writeString(file, Files.readString(file).replace("EMPLOYEE_UPDATE", "EMPLOYEE_DELETE"));
    assertThat(reader(dir).readAndVerify(DATE).valid()).isFalse();
}

@Test
void instance_id_prevents_two_nodes_sharing_a_file(@TempDir Path dir) {
    assertThat(writer(dir, "node-a", clock).pathFor(DATE))
            .isNotEqualTo(writer(dir, "node-b", clock).pathFor(DATE));
}
```

- [ ] **Step 2: Run tests and verify spool classes are missing**

Run: `mvn -q -Dtest=AuditSpoolWriterTest,AuditSpoolReaderTest test`

Expected: FAIL for missing writer/reader/hash verification.

- [ ] **Step 3: Implement canonical append, locking and verification**

Serialize payload with stable property ordering, compute `payloadHash`, carry `previousHash`, compute `lineHash = SHA-256(previousHash + payloadHash + eventId)`, acquire a JVM `ReentrantLock` per path plus `FileChannel.lock()`, append UTF-8 plus newline, and call `force(true)` before returning. Reader streams lines, validates all three hashes, reports duplicate `eventId` separately and never rewrites a damaged file.

Configure:

```yaml
audit:
  spool:
    directory: ${AUDIT_SPOOL_DIRECTORY:./var/audit-spool}
    instance-id: ${AUDIT_INSTANCE_ID:${HOSTNAME:local}}
    zone-id: Asia/Saigon
```

- [ ] **Step 4: Run spool tests**

Run: `mvn -q -Dtest=AuditSpoolWriterTest,AuditSpoolReaderTest test`

Expected: PASS for append order, fsync-visible content, duplicate IDs, tamper detection, separate instance files and timezone filename.

- [ ] **Step 5: Commit file durability support**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolProperties.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolLine.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolWriter.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolReader.java backend/fashion-system/src/main/resources/application.yaml backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolWriterTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/spool/AuditSpoolReaderTest.java
git commit -m "feat(audit): add tamper-evident daily spool"
```

### Task 5: Outbox dispatcher và acknowledged Kafka publishing

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditEventPublisher.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/KafkaAuditEventPublisher.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/NoopAuditEventPublisher.java`
- Delete: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AfterCommitAuditEventPublisher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/outbox/AuditOutboxDispatcher.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/outbox/AuditOutboxDeliveryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/KafkaAuditConfig.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/KafkaAuditEventPublisherTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/outbox/AuditOutboxDeliveryServiceTest.java`

**Interfaces:**
- Consumes: pending outbox rows, Task 4 spool writer and Kafka template acknowledgement.
- Produces: `CompletionStage<Void> AuditEventPublisher.publish(AuditEvent)`; scheduled dispatcher; retry metadata without throwing into request threads.

- [ ] **Step 1: Write tests for acknowledgement and partial delivery**

```java
@Test
void marks_file_before_kafka_and_only_marks_kafka_after_ack() {
    publisherFuture.completeExceptionally(new RuntimeException("broker down"));
    delivery.deliver(outbox);
    assertThat(reload(outbox).getFileAppendedAt()).isNotNull();
    assertThat(reload(outbox).getKafkaPublishedAt()).isNull();
    assertThat(reload(outbox).getAttemptCount()).isEqualTo(1);
}

@Test
void retry_does_not_append_again_after_file_was_marked() {
    outbox.markFileAppended(clock.instant());
    delivery.deliver(outbox);
    verify(spoolWriter, never()).append(any());
    verify(publisher).publish(any());
}
```

- [ ] **Step 2: Run dispatcher tests and observe current fire-and-forget failure**

Run: `mvn -q -Dtest=KafkaAuditEventPublisherTest,AuditOutboxDeliveryServiceTest test`

Expected: FAIL because publisher returns `void`, publish failure is swallowed, and no outbox dispatcher exists.

- [ ] **Step 3: Implement delivery states and backoff**

```java
public interface AuditEventPublisher {
    CompletionStage<Void> publish(AuditEvent event);
}
```

Map `KafkaTemplate.send(...).thenApply(result -> null)` into the returned stage. `NoopAuditEventPublisher` returns a failed stage so Kafka-disabled deployments retain pending outbox rows instead of falsely acknowledging them. Dispatcher polls a bounded batch using `FOR UPDATE SKIP LOCKED`; delivery service appends once, publishes, then updates `kafkaPublishedAt` only after broker acknowledgement. On failure set sanitized `lastError`, increment attempts and compute capped exponential `nextAttemptAt`.

- [ ] **Step 4: Run focused delivery tests**

Run: `mvn -q -Dtest=KafkaAuditEventPublisherTest,AuditOutboxDeliveryServiceTest test`

Expected: PASS for success, broker failure, retry, already-spooled rows and concurrent lock behavior.

- [ ] **Step 5: Commit durable dispatch**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditEventPublisher.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/KafkaAuditEventPublisher.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/NoopAuditEventPublisher.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AfterCommitAuditEventPublisher.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/outbox/AuditOutboxDispatcher.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/outbox/AuditOutboxDeliveryService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/KafkaAuditConfig.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/KafkaAuditEventPublisherTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/outbox/AuditOutboxDeliveryServiceTest.java
git commit -m "feat(audit): deliver outbox through file and Kafka"
```

### Task 6: Idempotent consumer và midnight reconciliation

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuditLogConsumerPersistence.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/KafkaAuditEventConsumer.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/AuditLogRepository.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationScheduler.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/AuditLogConsumerIdempotencyTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationServiceTest.java`

**Interfaces:**
- Consumes: request-level `AuditEvent`, verified daily files, `findExistingEventIds(Collection<UUID>)`.
- Produces: one append-only DB row per event; `ReconciliationResult reconcile(LocalDate date)`; cron `0 5 0 * * *` in `Asia/Saigon`.

- [ ] **Step 1: Extend failing consumer and reconciliation tests**

```java
@Test
void duplicate_event_id_is_success_without_second_insert() {
    assertThat(service.persist(event)).isTrue();
    assertThat(service.persist(event)).isFalse();
    assertThat(repository.countByEventId(event.eventId())).isEqualTo(1);
}

@Test
void republishes_missing_events_and_keeps_file() {
    when(repository.findExistingEventIds(Set.of(event1.eventId(), event2.eventId())))
            .thenReturn(Set.of(event1.eventId()));
    ReconciliationResult result = service.reconcile(DATE);
    verify(publisher).publish(event2);
    verify(fileOperations, never()).delete(any());
    assertThat(result.missing()).isEqualTo(1);
}

@Test
void deletes_only_valid_complete_previous_day_file() {
    when(clock.instant()).thenReturn(Instant.parse("2026-09-24T17:05:00Z"));
    when(repository.findExistingEventIds(any())).thenReturn(Set.of(event1.eventId(), event2.eventId()));
    scheduler.reconcilePreviousDay();
    verify(fileOperations).delete(reader.pathFor(LocalDate.of(2026, 9, 24)));
}
```

- [ ] **Step 2: Run tests and verify request-level persistence/reconciliation are absent**

Run: `mvn -q -Dtest=AuditLogConsumerIdempotencyTest,AuditReconciliationServiceTest test`

Expected: FAIL because consumer still maps metadata to `changedFields`, repository lacks batch lookup and no reconciliation exists.

- [ ] **Step 3: Implement consumer validation and reconciliation**

Consumer validates schema version, `eventId`, actor rules, `rowCount == changes.size()` and sanitized payload, then stores all request-level fields in `REQUIRES_NEW`. Treat unique-key duplicate as already persisted. Configure Kafka manual acknowledgement only after `persist()` returns; retain DLT behavior for invalid payload.

Reconciliation streams all instance files for the requested day, rejects invalid hash chains, deduplicates repeated IDs, queries existing IDs in batches of 500, republishes missing events and returns without deletion. It deletes files only on a later/full verification where missing count is zero and no related outbox row is unfinished.

- [ ] **Step 4: Run focused consumer and scheduler tests**

Run: `mvn -q -Dtest=AuditLogConsumerIdempotencyTest,AuditReconciliationServiceTest test`

Expected: PASS for duplicate delivery, batch lookup, missing replay, corrupted file retention, previous-day timezone and complete-file deletion.

- [ ] **Step 5: Commit persistence and reconciliation**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuditLogConsumerPersistence.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/KafkaAuditEventConsumer.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/AuditLogRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationScheduler.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/AuditLogConsumerIdempotencyTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationServiceTest.java
git commit -m "feat(audit): reconcile daily spool with audit database"
```

### Task 7: Phủ business mutation, bulk operations và dọn auth misuse

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthAuditService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AdminProfileService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/BrandService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CatalogMediaService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CategoryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CollectionService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerAddressService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerSocialAccountService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerTierAssignmentService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerTierService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/DepartmentService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsIssueService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsReceiptService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/InventoryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ModuleService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/OrderService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PaymentService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PermissionCatalogAdministrationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PositionService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductAttributeService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductImageService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductTagMappingService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductTagService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductVariantService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionConditionService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionScopeService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionUsageService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/RefundService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/RoleAssignmentAdministrationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ShipmentService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StockReservationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StoreService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StoreStaffService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/SupplierService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditInfrastructure.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BulkAuditRecorder.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/ProductRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/ProductVariantRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/InventoryBalanceRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/RolePermissionRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRoleRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserPermissionRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerAddressRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerTierAssignmentRepository.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/architecture/AuditMutationCoverageTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeBusinessAuditIntegrationTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/BulkAuditRecorderTest.java`

**Interfaces:**
- Consumes: `@BusinessAudit(domain)` and current collector.
- Produces: every public mutation boundary explicitly classified; `BulkAuditRecorder.record(table, rowId, operation, oldValues, newValues)` for JPQL/native bulk changes.

- [ ] **Step 1: Write architecture and employee regression tests**

```java
@Test
void every_transactional_mutation_service_is_audited_or_infrastructure() {
    Set<Method> uncovered = mutationMethods().stream()
            .filter(method -> !hasBusinessAudit(method) && !hasInfrastructureExclusion(method))
            .collect(toSet());
    assertThat(uncovered).isEmpty();
}

@Test
void employee_update_writes_business_outbox_but_not_auth_audit() {
    service.update(actorId, employeeId, request);
    verify(authAuditRepository, never()).save(any());
    AuditEvent event = onlyOutboxEvent();
    assertThat(event.action()).isEqualTo("EMPLOYEE_UPDATE");
    assertThat(event.changes()).extracting(AuditChange::table)
            .contains("users", "user_roles", "user_departments", "store_staff");
}

@Test
void auth_service_rejects_non_auth_action() {
    assertThatThrownBy(() -> authAuditService.record(userId, "EMPLOYEE_UPDATED", "x"))
            .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Run coverage tests and capture the full uncovered list**

Run: `mvn -q -Dtest=AuditMutationCoverageTest,EmployeeBusinessAuditIntegrationTest,BulkAuditRecorderTest test`

Expected: FAIL listing current mutation services, bulk repository methods and employee actions wrongly sent to `AuthAuditService`.

- [ ] **Step 3: Classify every mutation and adapt bulk writes**

Annotate business service types with stable domains such as `EMPLOYEE`, `STORE`, `PRODUCT`, `VARIANT`, `SUPPLIER`, `BRAND`, `CATEGORY`, `COLLECTION`, `INVENTORY`, `GOODS_RECEIPT`, `GOODS_ISSUE`, `CUSTOMER`, `ORDER`, `PAYMENT`, `REFUND`, `SHIPMENT`, `PROMOTION`, `ROLE` and `PERMISSION`. Derived actions use `DOMAIN_METHOD_NAME`; method annotation overrides irregular names like approve/restore.

Mark only `AuthService`, `RefreshTokenService`, `PasswordResetService`, `PaymentWebhookLogService` and other proven infrastructure boundaries with a non-empty reason in `@AuditInfrastructure(reason = "...")`. Remove employee constants and `recordTransactional` calls from `AuthAuditService`; enforce its three-action allowlist.

Before each JPQL/native bulk mutation, select affected IDs and old projections, execute the mutation, then call `BulkAuditRecorder` once per affected row with the resulting values. Replace bulk delete methods for role/user grants with entity loading and `deleteAll(entities)` where practical so Hibernate capture sees exact composite IDs.

- [ ] **Step 4: Run coverage plus representative domain tests**

Run: `mvn -q -Dtest=AuditMutationCoverageTest,EmployeeBusinessAuditIntegrationTest,BulkAuditRecorderTest,EmployeeAdministrationServiceTest,ProductVariantServiceTest,SupplierServiceTest test`

Expected: PASS with no uncovered mutation, no business action in auth audit, and complete row lists for bulk changes.

- [ ] **Step 5: Commit mutation rollout**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/AuditInfrastructure.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/BulkAuditRecorder.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthAuditService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/ProductRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/ProductVariantRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/InventoryBalanceRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/RolePermissionRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRoleRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserPermissionRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerAddressRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/CustomerTierAssignmentRepository.java
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AdminProfileService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/BrandService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CatalogMediaService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CategoryService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CollectionService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerAddressService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerSocialAccountService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerTierAssignmentService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/CustomerTierService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/DepartmentService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsIssueService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/GoodsReceiptService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/InventoryService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ModuleService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/OrderService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PaymentService.java
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PermissionCatalogAdministrationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PositionService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductAttributeService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductImageService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductTagMappingService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductTagService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ProductVariantService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionConditionService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionScopeService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PromotionUsageService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/RefundService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/RoleAssignmentAdministrationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/ShipmentService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StockReservationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StoreService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/StoreStaffService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/SupplierService.java
git add backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/architecture/AuditMutationCoverageTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeBusinessAuditIntegrationTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/BulkAuditRecorderTest.java
git commit -m "feat(audit): cover business mutations and separate auth history"
```

### Task 8: Tách query/API/UI của business audit và auth history

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/SystemAuditLogDto.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/AuthAuditLogResponse.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/SystemAuditLogQueryService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthAuditLogQueryService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/AuditLogController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/AuthAuditLogController.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V21__migrate_misclassified_auth_audit.sql`
- Modify: `frontend/react-app/src/api/adminLogsApi.js`
- Modify: `frontend/react-app/src/pages/admin/LoManagement.jsx`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/AuditLogControllerTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/AuthAuditLogControllerTest.java`
- Test: `frontend/react-app/src/api/adminLogsApi.test.js`

**Interfaces:**
- Consumes: separate repositories/tables and `LOG_VIEW`.
- Produces: `GET /api/admin/audit-logs` for business only and `GET /api/admin/auth-audit-logs` for auth only; business response includes `rowCount`, `changes`, request/job metadata.

- [ ] **Step 1: Write endpoint separation tests**

```java
@Test
@WithMockUser(authorities = "LOG_VIEW")
void business_endpoint_never_returns_auth_rows() throws Exception {
    mvc.perform(get("/api/admin/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].rowCount").value(2))
            .andExpect(jsonPath("$.content[0].category").doesNotExist());
    verifyNoInteractions(authAuditLogRepository);
}

@Test
@WithMockUser(authorities = "LOG_VIEW")
void auth_endpoint_reads_only_auth_history() throws Exception {
    mvc.perform(get("/api/admin/auth-audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].action").value("LOGIN_SUCCESS"));
    verifyNoInteractions(auditLogRepository);
}
```

- [ ] **Step 2: Run backend and frontend tests to verify current merged behavior fails**

Run: `mvn -q -Dtest=AuditLogControllerTest,AuthAuditLogControllerTest test`

Run: `cmd /c npm test -- --run src/api/adminLogsApi.test.js` from `frontend/react-app`

Expected: FAIL because the current service merges both tables and the auth endpoint/API client does not exist.

- [ ] **Step 3: Split queries and migrate old misclassified actions**

Business query paginates in SQL and supports actor, action, table/resource, row ID, request ID and time range. Auth query paginates `auth_audit_logs` and permits only login/logout actions. V21 copies legacy employee actions into request-shaped `audit_logs` with deterministic `event_id` and `migratedFromAuthAudit=true`; it does not delete original evidence.

Frontend exposes separate fetch functions and separate views/tabs labeled “Thao tác dữ liệu” and “Đăng nhập / đăng xuất”; neither client merges arrays locally.

- [ ] **Step 4: Run endpoint and client tests**

Run: `mvn -q -Dtest=AuditLogControllerTest,AuthAuditLogControllerTest test`

Run: `cmd /c npm test -- --run src/api/adminLogsApi.test.js` from `frontend/react-app`

Expected: PASS with strict table separation and permission checks.

- [ ] **Step 5: Commit query/UI separation**

```bash
git add backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/SystemAuditLogDto.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/AuthAuditLogResponse.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/SystemAuditLogQueryService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthAuditLogQueryService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/AuditLogController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/AuthAuditLogController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V21__migrate_misclassified_auth_audit.sql backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/AuditLogControllerTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/AuthAuditLogControllerTest.java frontend/react-app/src/api/adminLogsApi.js frontend/react-app/src/api/adminLogsApi.test.js
git add frontend/react-app/src/pages/admin/LoManagement.jsx
git commit -m "feat(audit): separate business and authentication history"
```

### Task 9: End-to-end failure tests, operations và final verification

**Files:**
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/ReliableAuditPipelineIntegrationTest.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/AuditSensitiveDataIntegrationTest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationRunner.java`
- Create: `backend/docs/audit/RELIABLE_BUSINESS_AUDIT.md`
- Modify: `docker-compose.audit.yml`
- Modify: `HUONG_DAN_CHAY_DU_AN.md`

**Interfaces:**
- Consumes: complete pipeline from annotated mutation through outbox/file/Kafka/DB/reconciliation.
- Produces: repeatable failure-mode tests, manual command `--audit.reconcile-date=YYYY-MM-DD`, operational metrics/runbook.

- [ ] **Step 1: Write end-to-end acceptance tests**

```java
@Test
void kafka_outage_keeps_business_success_and_replays_same_event_id() {
    UUID eventId = executeEmployeeMutationWithKafkaUnavailable();
    assertThat(employeeRepository.findById(employeeId)).isPresent();
    assertThat(outboxRepository.findByEventId(eventId)).isPresent();
    assertThat(spoolReader.readAndVerify(today).eventIds()).contains(eventId);
    restoreKafkaAndDispatch();
    assertThat(auditLogRepository.countByEventId(eventId)).isEqualTo(1);
}

@Test
void secrets_are_absent_from_every_delivery_stage() {
    executePasswordChangingMutation();
    assertThat(outboxJson()).doesNotContain(rawPassword, token, otp);
    assertThat(spoolText()).doesNotContain(rawPassword, token, otp);
    assertThat(kafkaPayload()).doesNotContain(rawPassword, token, otp);
    assertThat(auditLogJson()).doesNotContain(rawPassword, token, otp);
}
```

- [ ] **Step 2: Run acceptance tests and verify any integration gaps fail explicitly**

Run: `mvn -q -Dtest=ReliableAuditPipelineIntegrationTest,AuditSensitiveDataIntegrationTest test`

Expected: FAIL until scheduling configuration, manual runner, Kafka/container wiring and all delivery stages satisfy the assertions.

- [ ] **Step 3: Complete operations wiring and documentation**

Add Kafka health/readiness notes without making Kafka readiness block business API startup. Document spool ownership/permissions, disk-full alert, pending-outbox alert, DLT inspection, manual reconciliation, recovery sequence and the fact that a user with control of DB/filesystem/keys is outside the evidence trust boundary. Add Prometheus-compatible gauges/counters through Micrometer only if the project already exposes Actuator; otherwise expose structured logs and repository health counts without a new dependency.

The manual runner accepts exactly an ISO date, calls `reconcile(date)`, prints counts without payloads and exits non-zero for invalid hash, missing events or infrastructure errors.

- [ ] **Step 4: Run fresh full verification**

Run: `mvn test -q` from `backend/fashion-system`

Run: `cmd /c npm test -- --run` from `frontend/react-app`

Run: `cmd /c npm run build` from `frontend/react-app`

Expected: all backend tests pass, all frontend tests pass and production build exits 0. If an unrelated pre-existing test fails, record its exact test name/output and do not claim a green suite.

- [ ] **Step 5: Commit operations and acceptance coverage**

```bash
git add backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/ReliableAuditPipelineIntegrationTest.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/audit/AuditSensitiveDataIntegrationTest.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/audit/reconcile/AuditReconciliationRunner.java backend/docs/audit/RELIABLE_BUSINESS_AUDIT.md docker-compose.audit.yml HUONG_DAN_CHAY_DU_AN.md
git commit -m "test(audit): verify reliable audit delivery end to end"
```

## Final acceptance checklist

- [ ] `auth_audit_logs` receives only login success/failure/logout.
- [ ] Every business mutation service is classified by the architecture test.
- [ ] One successful request/job produces one event with exact row count and row IDs.
- [ ] Rollback produces neither outbox nor business audit.
- [ ] Kafka outage does not fail business API and cannot lose the committed outbox event.
- [ ] File hash corruption prevents deletion and raises an operational error.
- [ ] Midnight reconciliation republishes missing IDs and deletes only complete valid files.
- [ ] Consumer retry and file duplicate lines produce only one `audit_logs` row per `eventId`.
- [ ] Business and auth endpoints/UI remain separate.
- [ ] No secret appears in outbox, file, Kafka or DB audit payloads.
- [ ] Full backend suite, frontend suite and frontend production build have fresh results.
