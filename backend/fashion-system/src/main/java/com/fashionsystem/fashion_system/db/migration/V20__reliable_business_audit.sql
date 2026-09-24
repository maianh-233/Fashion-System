CREATE TABLE audit_outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL UNIQUE,
  payload JSONB NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  file_appended_at TIMESTAMPTZ,
  kafka_published_at TIMESTAMPTZ,
  attempt_count INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ,
  last_error TEXT
);

CREATE INDEX idx_audit_outbox_pending
  ON audit_outbox(next_attempt_at, occurred_at, event_id)
  WHERE file_appended_at IS NULL OR kafka_published_at IS NULL;

ALTER TABLE audit_logs
  ADD COLUMN actor_type VARCHAR(16) CHECK (actor_type IN ('EMPLOYEE', 'SYSTEM')),
  ADD COLUMN request_id VARCHAR(128),
  ADD COLUMN method VARCHAR(16),
  ADD COLUMN path TEXT,
  ADD COLUMN job_name VARCHAR(128),
  ADD COLUMN row_count INTEGER CHECK (row_count >= 0),
  ADD COLUMN changes JSONB,
  ADD COLUMN occurred_at TIMESTAMPTZ;

-- Preserve legacy single-row evidence in the new request-level shape.
DROP TRIGGER audit_logs_append_only ON audit_logs;

UPDATE audit_logs
SET actor_type = CASE WHEN actor_user_id IS NULL THEN 'SYSTEM' ELSE 'EMPLOYEE' END,
    row_count = 1,
    changes = jsonb_build_array(jsonb_build_object(
      'table', entity_type,
      'rowId', entity_id::text,
      'operation', CASE
        WHEN old_data IS NULL AND new_data IS NOT NULL THEN 'INSERT'
        WHEN new_data IS NULL AND old_data IS NOT NULL THEN 'DELETE'
        ELSE 'UPDATE' END,
      'changedFields', COALESCE(changed_fields, '[]'::jsonb),
      'oldValues', old_data,
      'newValues', new_data)),
    occurred_at = created_at AT TIME ZONE 'UTC';

CREATE TRIGGER audit_logs_append_only
  BEFORE UPDATE OR DELETE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION reject_audit_log_mutation();

ALTER TABLE audit_logs
  ALTER COLUMN actor_user_id DROP NOT NULL,
  ALTER COLUMN entity_type DROP NOT NULL,
  ALTER COLUMN entity_id DROP NOT NULL,
  ALTER COLUMN changed_fields DROP NOT NULL;

CREATE INDEX idx_audit_logs_changes_gin ON audit_logs USING GIN (changes);
CREATE INDEX idx_audit_logs_actor_action_occurred
  ON audit_logs (actor_user_id, action, occurred_at DESC);
CREATE INDEX idx_audit_logs_request_id ON audit_logs (request_id);
