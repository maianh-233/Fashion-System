# Reliable business audit operations

## Startup and storage

Start PostgreSQL and configure the backend database as usual. Kafka is optional for local startup. For Kafka audit delivery, start the broker from the repository root with `docker compose -f docker-compose.audit.yml up -d audit-kafka`, then set `AUDIT_KAFKA_ENABLED=true` and `AUDIT_KAFKA_BOOTSTRAP_SERVERS=localhost:9092` in `backend/fashion-system/.env`. The business API must be able to start while Kafka is unavailable; a broker readiness check must not be an API startup dependency.

Set `AUDIT_SPOOL_DIRECTORY` to durable local storage with enough free space for a broker outage, and `AUDIT_INSTANCE_ID` to a stable, distinct value for each backend instance. The process account needs read, create, append, and delete access; other accounts should not have write access. Persist and back up the directory independently of the Kafka container. Do not put it on ephemeral container storage. The default `./var/audit-spool` is relative to the backend process working directory.

The Docker Compose file persists Kafka data in `audit_kafka_data`. It exposes an unauthenticated plaintext listener on localhost for local development; secure the broker and its network before using it outside that environment.

## Watch and recover

- Alert on low or exhausted free space and any spool append/read/hash failure. Disk-full errors can prevent durable audit capture; restore space and investigate affected business requests before retrying.
- Alert when `audit_outbox` rows with `file_appended_at IS NULL` or `kafka_published_at IS NULL` remain pending, especially when the count or oldest age grows. The dispatcher retries; do not delete pending rows to clear an alert.
- Alert on Kafka dead-letter topic records (the configured audit topic with `.DLT` suffix) and consumer errors. Investigate the cause and replay deliberately after repair; a DLT record is not proof of an audit row in PostgreSQL.

For a Kafka outage, keep the backend and spool storage running, restore broker connectivity, then watch the pending outbox count fall and consumer lag drain. Check the DLT before declaring recovery. Reconcile each affected calendar day after delivery catches up. Do not remove spool files manually. If a hash chain is invalid, preserve the original file for investigation; do not edit and retry it.

## Manual reconciliation

From `backend/fashion-system`, using the same `.env`, database, Kafka, and spool configuration as the backend, run:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.main-class=com.fashionsystem.fashion_system.audit.reconcile.AuditReconciliationRunner" "-Dspring-boot.run.arguments=--audit.reconcile-date=2026-09-23"
```

Replace the date with the required `YYYY-MM-DD` audit calendar day. The runner accepts exactly one date option. It prints only file, distinct event, missing-record, and pending-outbox counts; it never prints event payloads. Exit code is nonzero for an invalid hash chain, absent spool file, pending outbox, missing audit record, or infrastructure failure. Missing records are republished; wait for the consumer, then rerun until the command succeeds. Keep the spool until the scheduled verification removes it.

The scheduler runs at 00:05 in `Asia/Saigon` for the previous day and revisits retained older days. An event close to midnight belongs to the file selected by the spool writer's configured zone and append time. Allow in-flight outbox writes to finish before reconciling a day; the check defers cleanup while any outbox row is incomplete. The first clean pass marks a day verified, and a later clean pass removes its spool files. A standalone manual invocation does not carry that in-memory verification state into the next invocation.

## Trust boundary

The hash chain detects modification of retained spool lines relative to their neighbors; it is not a digital signature or an independent witness. Someone able to rewrite an entire spool file, delete it, or change both the spool and database can defeat local verification. Restrict storage and database privileges, retain independent backups, and investigate gaps using source business data and broker records. Kafka delivery and the existence of a spool line alone do not prove that the audit row has been persisted.
