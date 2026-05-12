-- =========================================================
-- V1__init_payment_service.sql
-- Payment Service Database Migration
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- TABLE: payments
-- =========================================================
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    payment_code VARCHAR(50) UNIQUE,

    method VARCHAR(50) NOT NULL,

    amount DECIMAL(14,2) NOT NULL CHECK (amount >= 0),

    status VARCHAR(50) NOT NULL,

    transaction_code VARCHAR(100),

    paid_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

-- =========================================================
-- TABLE: payment_transactions
-- =========================================================
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    payment_id UUID NOT NULL,

    gateway_transaction_id VARCHAR(100),

    transaction_type VARCHAR(30) NOT NULL,

    amount DECIMAL(14,2) NOT NULL CHECK (amount >= 0),

    status VARCHAR(50) NOT NULL,

    raw_response JSONB,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_transactions_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: refunds
-- =========================================================
CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    payment_id UUID NOT NULL,

    refund_code VARCHAR(50) UNIQUE,

    amount DECIMAL(14,2) NOT NULL CHECK (amount >= 0),

    reason TEXT,

    status VARCHAR(50) NOT NULL,

    requested_by UUID,

    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    processed_at TIMESTAMP,

    CONSTRAINT fk_refunds_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: payment_webhook_logs
-- =========================================================
CREATE TABLE payment_webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    provider VARCHAR(50) NOT NULL,

    event_type VARCHAR(100),

    payload JSONB NOT NULL,

    processed BOOLEAN DEFAULT FALSE,

    processed_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_payments_order_id
    ON payments(order_id);

CREATE INDEX idx_payments_status
    ON payments(status);

CREATE INDEX idx_payments_payment_code
    ON payments(payment_code);

CREATE INDEX idx_payment_transactions_payment_id
    ON payment_transactions(payment_id);

CREATE INDEX idx_payment_transactions_gateway_transaction_id
    ON payment_transactions(gateway_transaction_id);

CREATE INDEX idx_payment_transactions_status
    ON payment_transactions(status);

CREATE INDEX idx_refunds_payment_id
    ON refunds(payment_id);

CREATE INDEX idx_refunds_status
    ON refunds(status);

CREATE INDEX idx_payment_webhook_logs_provider
    ON payment_webhook_logs(provider);

CREATE INDEX idx_payment_webhook_logs_processed
    ON payment_webhook_logs(processed);

-- =========================================================
-- COMMENTS
-- =========================================================

COMMENT ON TABLE payments IS 'Store payment information for orders';

COMMENT ON TABLE payment_transactions IS
'Store payment gateway transaction history';

COMMENT ON TABLE refunds IS
'Store refund requests and refund processing information';

COMMENT ON TABLE payment_webhook_logs IS
'Store webhook logs received from payment providers';