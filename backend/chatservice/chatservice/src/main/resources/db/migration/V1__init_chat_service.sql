-- =========================================================
-- V1__init_chat_service.sql
-- Chat Service Database Migration
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 1. ORDER CHAT ROOMS
-- =========================================================

CREATE TABLE order_chat_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,

    customer_id UUID,

    assigned_staff_id UUID,

    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',

    last_message_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    closed_at TIMESTAMP
);

-- =========================================================
-- 2. ORDER CHAT MESSAGES
-- =========================================================

CREATE TABLE order_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    room_id UUID NOT NULL,

    sender_id UUID,

    sender_type VARCHAR(20) NOT NULL,

    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',

    content TEXT,

    related_action VARCHAR(50),

    metadata JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_chat_messages_room
        FOREIGN KEY (room_id)
        REFERENCES order_chat_rooms(id)
        ON DELETE CASCADE
);

-- =========================================================
-- 3. ORDER CHAT MESSAGE STATUS
-- =========================================================

CREATE TABLE order_chat_message_status (
    message_id UUID NOT NULL,

    user_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'SENT',

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (message_id, user_id),

    CONSTRAINT fk_order_chat_message_status_message
        FOREIGN KEY (message_id)
        REFERENCES order_chat_messages(id)
        ON DELETE CASCADE
);

-- =========================================================
-- 4. ORDER ISSUE TYPES
-- =========================================================

CREATE TABLE order_issue_types (
    code VARCHAR(50) PRIMARY KEY,

    name VARCHAR(255) NOT NULL
);

-- =========================================================
-- 5. INTERNAL CHAT ROOMS
-- =========================================================

CREATE TABLE internal_chat_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_by UUID,

    room_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',

    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    closed_at TIMESTAMP
);

-- =========================================================
-- 6. INTERNAL CHAT MESSAGES
-- =========================================================

CREATE TABLE internal_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    room_id UUID NOT NULL,

    sender_id UUID,

    sender_type VARCHAR(20),

    content TEXT,

    intent VARCHAR(100),

    metadata JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_internal_chat_messages_room
        FOREIGN KEY (room_id)
        REFERENCES internal_chat_rooms(id)
        ON DELETE CASCADE
);

-- =========================================================
-- 7. CHAT ATTACHMENTS
-- =========================================================

CREATE TABLE chat_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    message_id UUID NOT NULL,

    file_name VARCHAR(255),

    file_url TEXT NOT NULL,

    file_type VARCHAR(100),

    file_size BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_attachments_message
        FOREIGN KEY (message_id)
        REFERENCES order_chat_messages(id)
        ON DELETE CASCADE
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_order_chat_messages_room_id
    ON order_chat_messages(room_id);

CREATE INDEX idx_order_chat_messages_created_at
    ON order_chat_messages(created_at DESC);

CREATE INDEX idx_order_chat_room_order_id
    ON order_chat_rooms(order_id);

CREATE INDEX idx_order_chat_room_customer_id
    ON order_chat_rooms(customer_id);

CREATE INDEX idx_order_chat_room_staff_id
    ON order_chat_rooms(assigned_staff_id);

CREATE INDEX idx_internal_chat_messages_room_id
    ON internal_chat_messages(room_id);

CREATE INDEX idx_internal_chat_messages_created_at
    ON internal_chat_messages(created_at DESC);

CREATE INDEX idx_chat_attachments_message_id
    ON chat_attachments(message_id);

-- =========================================================
-- DEFAULT ISSUE TYPES
-- =========================================================

INSERT INTO order_issue_types (code, name)
VALUES
    ('LATE_DELIVERY', 'Late Delivery'),
    ('WRONG_PRODUCT', 'Wrong Product'),
    ('DAMAGED_PRODUCT', 'Damaged Product'),
    ('REFUND_REQUEST', 'Refund Request'),
    ('CANCEL_ORDER', 'Cancel Order'),
    ('PAYMENT_ISSUE', 'Payment Issue');

-- =========================================================
-- COMMENTS
-- =========================================================

COMMENT ON TABLE order_chat_rooms IS 'Chat rooms related to customer orders';

COMMENT ON TABLE order_chat_messages IS 'Messages exchanged in order chat rooms';

COMMENT ON TABLE order_chat_message_status IS 'Message delivery/read status per user';

COMMENT ON TABLE order_issue_types IS 'Master data for order issue categories';

COMMENT ON TABLE internal_chat_rooms IS 'Internal communication rooms for staff/admin';

COMMENT ON TABLE internal_chat_messages IS 'Messages inside internal chat rooms';

COMMENT ON TABLE chat_attachments IS 'Attachments linked to chat messages';
