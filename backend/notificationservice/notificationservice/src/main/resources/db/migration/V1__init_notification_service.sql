-- =========================================================
-- V1__init_notification_service.sql
-- Notification Service Database Migration
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- TABLE: notifications
-- =========================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    recipient_id UUID,

    recipient_type VARCHAR(20) NOT NULL,

    channel VARCHAR(20) NOT NULL,

    title VARCHAR(255),

    content TEXT NOT NULL,

    reference_type VARCHAR(50),

    reference_id UUID,

    status VARCHAR(20) DEFAULT 'PENDING',

    scheduled_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    sent_at TIMESTAMP
);

-- =========================================================
-- TABLE: notification_logs
-- =========================================================
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    notification_id UUID NOT NULL,

    channel VARCHAR(20) NOT NULL,

    destination VARCHAR(255),

    status VARCHAR(20) NOT NULL,

    error_message TEXT,

    provider VARCHAR(50),

    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_logs_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: notification_templates
-- =========================================================
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) UNIQUE NOT NULL,

    title_template VARCHAR(255),

    content_template TEXT NOT NULL,

    channel VARCHAR(20) NOT NULL,

    active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

-- =========================================================
-- TABLE: user_notification_preferences
-- =========================================================
CREATE TABLE user_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    channel VARCHAR(20) NOT NULL,

    enabled BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP,

    CONSTRAINT uk_user_notification_channel
        UNIQUE (user_id, channel)
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_notifications_recipient_id
    ON notifications(recipient_id);

CREATE INDEX idx_notifications_reference
    ON notifications(reference_type, reference_id);

CREATE INDEX idx_notifications_status
    ON notifications(status);

CREATE INDEX idx_notifications_channel
    ON notifications(channel);

CREATE INDEX idx_notifications_created_at
    ON notifications(created_at);

CREATE INDEX idx_notification_logs_notification_id
    ON notification_logs(notification_id);

CREATE INDEX idx_notification_logs_status
    ON notification_logs(status);

CREATE INDEX idx_notification_logs_channel
    ON notification_logs(channel);

CREATE INDEX idx_notification_templates_code
    ON notification_templates(code);

CREATE INDEX idx_notification_templates_channel
    ON notification_templates(channel);

CREATE INDEX idx_user_notification_preferences_user_id
    ON user_notification_preferences(user_id);

CREATE INDEX idx_user_notification_preferences_channel
    ON user_notification_preferences(channel);

-- =========================================================
-- SAMPLE DATA (OPTIONAL)
-- =========================================================

INSERT INTO notification_templates (
    code,
    title_template,
    content_template,
    channel
)
VALUES
(
    'ORDER_SUCCESS',
    'Order Created Successfully',
    'Hello {{customer_name}}, your order {{order_code}} has been created successfully.',
    'EMAIL'
),
(
    'RESET_PASSWORD',
    'Reset Password',
    'Click the link below to reset your password.',
    'EMAIL'
),
(
    'PAYMENT_SUCCESS',
    'Payment Successful',
    'Your payment for order {{order_code}} was successful.',
    'PUSH'
);