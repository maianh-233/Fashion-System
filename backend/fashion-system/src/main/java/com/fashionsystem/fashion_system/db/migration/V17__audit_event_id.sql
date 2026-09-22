ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS event_id UUID;
UPDATE audit_logs SET event_id = gen_random_uuid() WHERE event_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_audit_logs_event_id ON audit_logs(event_id);
