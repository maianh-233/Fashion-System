-- Read state for in-app notifications (bell badge, mark read / mark all read).

BEGIN;

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at timestamp;
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread
    ON notifications(recipient_type, recipient_id) WHERE read_at IS NULL;

COMMIT;
