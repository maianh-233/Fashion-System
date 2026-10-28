-- Online orders: duplicate-submit protection for customer checkout and the
-- history of staff contacting a customer about an order.

BEGIN;

-- 1. The checkout page sends one random key per submit attempt; pressing "Đặt hàng"
--    twice (or a network retry) returns the first order instead of creating another.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS client_request_id varchar(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_customer_request
    ON orders(customer_id, client_request_id) WHERE client_request_id IS NOT NULL;

-- 2. Staff contacting the customer (email and/or in-app notification). Delivery
--    happens after the transaction commits; the status columns record the outcome.
CREATE TABLE IF NOT EXISTS order_customer_contacts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders(id),
    customer_id uuid NOT NULL REFERENCES customers(id),
    sent_by uuid NOT NULL REFERENCES users(id),
    subject varchar(200) NOT NULL,
    content text NOT NULL,
    via_email boolean NOT NULL DEFAULT false,
    via_notification boolean NOT NULL DEFAULT false,
    email_to varchar(255),
    email_status varchar(20) NOT NULL DEFAULT 'SKIPPED',
    email_error text,
    notification_status varchar(20) NOT NULL DEFAULT 'SKIPPED',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_contacts_channel CHECK (via_email OR via_notification),
    CONSTRAINT chk_order_contacts_email_status CHECK (email_status IN ('SKIPPED','PENDING','SENT','FAILED')),
    CONSTRAINT chk_order_contacts_notification_status CHECK (notification_status IN ('SKIPPED','PENDING','SENT','FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_order_customer_contacts_order ON order_customer_contacts(order_id, created_at DESC);

-- 3. changed_by holds a users.id (actor_type USER) or a customers.id (actor_type CUSTOMER),
--    so the legacy FK to users cannot stay; actor_type identifies the referenced table.
ALTER TABLE order_status_histories DROP CONSTRAINT IF EXISTS fk_order_status_histories_changed_by;

COMMIT;
