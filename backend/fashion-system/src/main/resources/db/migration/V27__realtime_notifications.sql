BEGIN;

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS event_id uuid;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS actor_user_id uuid;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type varchar(80);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS action_url varchar(500);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS metadata jsonb;

UPDATE notifications SET event_id = id WHERE event_id IS NULL;
UPDATE notifications SET type = 'SYSTEM' WHERE type IS NULL;

ALTER TABLE notifications ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE notifications ALTER COLUMN type SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_event_recipient
    ON notifications(event_id, recipient_type, recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications(recipient_type, recipient_id, created_at DESC);

COMMIT;
