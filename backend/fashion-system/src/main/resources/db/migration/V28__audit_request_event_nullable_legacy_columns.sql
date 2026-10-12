-- Request-level audit events store row changes in audit_logs.changes.
-- Legacy single-row columns remain available for old records, but are no
-- longer mandatory for new multi-row events.
ALTER TABLE audit_logs
  ALTER COLUMN actor_user_id DROP NOT NULL,
  ALTER COLUMN entity_type DROP NOT NULL,
  ALTER COLUMN entity_id DROP NOT NULL,
  ALTER COLUMN changed_fields DROP NOT NULL;
