-- Chiến dịch marketing — Phase 2: đợt gửi thông báo trong app. Một chiến dịch có nhiều đợt, mỗi đợt có nhóm khách, nội dung
-- và giờ gửi riêng. Trạng thái đợt tách khỏi trạng thái chiến dịch. Danh sách người nhận được chốt đúng lúc bắt đầu gửi
-- (marketing_dispatch_recipients), mỗi người một dòng có trạng thái / số lần thử / lỗi riêng; event_id cố định cho mỗi
-- (đợt, khách) nên chạy lại sau sự cố không tạo thông báo thứ hai (uk_notifications_event_recipient có sẵn).
-- Kênh duy nhất là IN_APP; EMAIL chỉ thêm khi có cơ chế đồng ý nhận tin và hủy đăng ký.

CREATE SEQUENCE marketing_dispatch_code_seq;

CREATE TABLE marketing_dispatches (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id       UUID NOT NULL REFERENCES marketing_campaigns(id),
    code              VARCHAR(30) NOT NULL UNIQUE,
    channel           VARCHAR(20) NOT NULL DEFAULT 'IN_APP' CHECK (channel IN ('IN_APP')),
    title             VARCHAR(120) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    action_url        VARCHAR(500),
    -- Tiêu chí nhóm khách (cùng ý nghĩa với VoucherCampaignDtos.Criteria) + nhãn lúc tạo để hiển thị khi hạng / danh mục đổi tên.
    criteria          JSONB NOT NULL,
    scheduled_at      TIMESTAMP NOT NULL,
    status            VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'SCHEDULED', 'SENDING',
                          'COMPLETED', 'COMPLETED_WITH_ERRORS', 'CANCELLED')),
    snapshot_at       TIMESTAMP,
    started_at        TIMESTAMP,
    finished_at       TIMESTAMP,
    audience_count    INTEGER,
    sent_count        INTEGER,
    skipped_count     INTEGER,
    failed_count      INTEGER,
    stall_notified_at TIMESTAMP,
    approved_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at       TIMESTAMP,
    cancelled_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at      TIMESTAMP,
    cancel_reason     VARCHAR(500),
    created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at        TIMESTAMP
);
CREATE INDEX idx_marketing_dispatches_due ON marketing_dispatches (scheduled_at) WHERE status = 'SCHEDULED';
CREATE INDEX idx_marketing_dispatches_sending ON marketing_dispatches (started_at) WHERE status = 'SENDING';
CREATE INDEX idx_marketing_dispatches_campaign ON marketing_dispatches (campaign_id, scheduled_at);

CREATE TABLE marketing_dispatch_recipients (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispatch_id      UUID NOT NULL REFERENCES marketing_dispatches(id),
    customer_id      UUID NOT NULL REFERENCES customers(id),
    event_id         UUID NOT NULL UNIQUE,
    status           VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'SKIPPED')),
    skip_reason      VARCHAR(30) CHECK (skip_reason IN ('NO_ACCOUNT', 'INACTIVE', 'FREQUENCY_CAP', 'CANCELLED',
                         'CAMPAIGN_NOT_ACTIVE')),
    attempt_count    INTEGER NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMP,
    lease_until      TIMESTAMP,
    sent_at          TIMESTAMP,
    last_error_code  VARCHAR(50),
    last_error       VARCHAR(500),
    -- Chỉ tham chiếu (không FK) để không ràng buộc vòng đời bảng notifications.
    notification_id  UUID,
    created_at       TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_marketing_dispatch_recipient UNIQUE (dispatch_id, customer_id),
    CONSTRAINT chk_marketing_dispatch_recipient_skip CHECK ((status = 'SKIPPED') = (skip_reason IS NOT NULL))
);
CREATE INDEX idx_marketing_dispatch_recipients_status ON marketing_dispatch_recipients (dispatch_id, status);
CREATE INDEX idx_marketing_dispatch_recipients_work ON marketing_dispatch_recipients (next_attempt_at)
    WHERE status IN ('PENDING', 'SENDING');
-- Giới hạn tần suất: khách đã nhận thông báo marketing trong N giờ gần nhất.
CREATE INDEX idx_marketing_dispatch_recipients_sent ON marketing_dispatch_recipients (customer_id, sent_at DESC)
    WHERE status = 'SENT';

ALTER TABLE marketing_campaign_history DROP CONSTRAINT marketing_campaign_history_action_check;
ALTER TABLE marketing_campaign_history ADD CONSTRAINT marketing_campaign_history_action_check CHECK (action IN (
    'CREATED', 'UPDATED', 'SUBMITTED', 'WITHDRAWN', 'APPROVED', 'REJECTED', 'REOPENED', 'CANCELLED', 'OFFER_LINKED',
    'OFFER_UNLINKED', 'EXPENSE_ADDED', 'EXPENSE_UPDATED', 'EXPENSE_DELETED', 'DISPATCH_CREATED', 'DISPATCH_UPDATED',
    'DISPATCH_APPROVED', 'DISPATCH_REJECTED', 'DISPATCH_CANCELLED', 'DISPATCH_STARTED', 'DISPATCH_FINISHED',
    'DISPATCH_RETRIED'));
