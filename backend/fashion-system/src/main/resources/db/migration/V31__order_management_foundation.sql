-- Order management foundation: normalized order statuses/types, handler and
-- soft-hide tracking, split-order parent link, generated order codes,
-- actor-aware status history and the order permissions used by the admin module.

BEGIN;

-- 1. Orders table had no rows when this module started; stop instead of
--    silently rewriting legacy values (PROCESSING, DELIVERED, PICKUP...).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM orders WHERE status NOT IN (
            'PENDING','CONFIRMED','HANDED_TO_SHIPPER','SHIPPING','COMPLETED',
            'DELIVERY_FAILED','RETURNED','CANCELLED'))
        OR EXISTS (SELECT 1 FROM orders WHERE order_type NOT IN ('ONLINE','OFFLINE')) THEN
        RAISE EXCEPTION 'Order migration stopped: orders with legacy status or order_type';
    END IF;
END $$;

ALTER TABLE orders
    -- Split online orders: the customer's order is the parent, each store handles a child.
    ADD COLUMN IF NOT EXISTS parent_order_id uuid,
    -- Employee currently responsible for the order (creator of an offline order, confirmer of an online one).
    ADD COLUMN IF NOT EXISTS handler_user_id uuid,
    ADD COLUMN IF NOT EXISTS created_by uuid,
    ADD COLUMN IF NOT EXISTS payment_method varchar(30),
    -- Offline cash payment: amount handed over by the customer and change returned.
    ADD COLUMN IF NOT EXISTS cash_received numeric(14,2),
    ADD COLUMN IF NOT EXISTS change_amount numeric(14,2),
    ADD COLUMN IF NOT EXISTS cancel_reason text,
    ADD COLUMN IF NOT EXISTS cancelled_by_type varchar(20),
    -- Soft hide: hidden orders stay in the database and can be restored.
    ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS hidden_by uuid,
    ADD COLUMN IF NOT EXISTS hidden_at timestamp,
    ADD COLUMN IF NOT EXISTS restored_by uuid,
    ADD COLUMN IF NOT EXISTS restored_at timestamp;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_parent') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_parent FOREIGN KEY (parent_order_id) REFERENCES orders(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_handler') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_handler FOREIGN KEY (handler_user_id) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_created_by') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_hidden_by') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_hidden_by FOREIGN KEY (hidden_by) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_restored_by') THEN
        ALTER TABLE orders ADD CONSTRAINT fk_orders_restored_by FOREIGN KEY (restored_by) REFERENCES users(id);
    END IF;
END $$;

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN (
    'PENDING','CONFIRMED','HANDED_TO_SHIPPER','SHIPPING','COMPLETED','DELIVERY_FAILED','RETURNED','CANCELLED'));
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_type;
ALTER TABLE orders ADD CONSTRAINT chk_orders_type CHECK (order_type IN ('ONLINE','OFFLINE'));
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_cancelled_by_type;
ALTER TABLE orders ADD CONSTRAINT chk_orders_cancelled_by_type
    CHECK (cancelled_by_type IS NULL OR cancelled_by_type IN ('USER','CUSTOMER','SYSTEM','SHIPPER'));
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_cash;
ALTER TABLE orders ADD CONSTRAINT chk_orders_cash
    CHECK ((cash_received IS NULL OR cash_received >= 0) AND (change_amount IS NULL OR change_amount >= 0));
-- Only finished orders may be hidden.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_hidden_terminal;
ALTER TABLE orders ADD CONSTRAINT chk_orders_hidden_terminal
    CHECK (active OR status IN ('COMPLETED','CANCELLED','RETURNED'));

CREATE INDEX IF NOT EXISTS idx_orders_store_type_created ON orders(store_id, order_type, active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_parent ON orders(parent_order_id);
CREATE INDEX IF NOT EXISTS idx_orders_handler ON orders(handler_user_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);

-- 2. Order codes LB-yyMMdd-000123: the numeric part comes from this sequence so
--    concurrent creators never collide (nextval is atomic and never rolled back).
CREATE SEQUENCE IF NOT EXISTS order_code_seq START WITH 1 INCREMENT BY 1;

-- 3. Status history records who changed the status: an employee, the customer,
--    the system or the shipping provider (changed_by holds the user/customer id).
ALTER TABLE order_status_histories
    ADD COLUMN IF NOT EXISTS actor_type varchar(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS reason text;
ALTER TABLE order_status_histories DROP CONSTRAINT IF EXISTS chk_order_status_histories_actor;
ALTER TABLE order_status_histories ADD CONSTRAINT chk_order_status_histories_actor
    CHECK (actor_type IN ('USER','CUSTOMER','SYSTEM','SHIPPER'));
CREATE INDEX IF NOT EXISTS idx_order_status_histories_order ON order_status_histories(order_id, changed_at);

-- 4. Order permissions. ORDER_VIEW/CREATE/CONFIRM/CANCEL already exist;
--    ORDER_CREATE is the offline (counter) order permission.
INSERT INTO permissions(id,code,name,group_id,description,created_at)
SELECT (
    substr(md5('permission:' || action.code),1,8) || '-' || substr(md5('permission:' || action.code),9,4) || '-' ||
    substr(md5('permission:' || action.code),13,4) || '-' || substr(md5('permission:' || action.code),17,4) || '-' ||
    substr(md5('permission:' || action.code),21,12)
)::uuid, action.code, action.name, g.id, action.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('ORDER_HANDOVER_SHIPPING','Giao đơn cho vận chuyển','Tạo chuyến giao hàng và bàn giao đơn cho bên vận chuyển'),
    ('ORDER_HIDE','Ẩn đơn hàng','Ẩn đơn hàng đã kết thúc khỏi danh sách'),
    ('ORDER_RESTORE','Khôi phục đơn hàng','Khôi phục đơn hàng đã ẩn'),
    ('ORDER_CONTACT_CUSTOMER','Liên hệ khách hàng','Gửi email hoặc thông báo cho khách về đơn hàng'),
    ('ORDER_SUPPORT_CHAT','Hỗ trợ đơn hàng','Nhận và trả lời hội thoại hỗ trợ đơn hàng'),
    ('ORDER_RETURN_OVERRIDE','Xác nhận hoàn hàng thay người xử lý','Xác nhận đã nhận hàng hoàn cho đơn do nhân viên khác xử lý')
) action(code,name,description)
WHERE g.code='ORDER'
ON CONFLICT(code) DO NOTHING;

-- Existing order grants use scope ALL; the employee's store assignment still limits the data.
INSERT INTO role_permissions(role_id,permission_id,scope)
SELECT r.id,p.id,'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN','ADMIN','MANAGER')
  AND p.code IN ('ORDER_HANDOVER_SHIPPING','ORDER_HIDE','ORDER_RESTORE','ORDER_CONTACT_CUSTOMER',
                 'ORDER_SUPPORT_CHAT','ORDER_RETURN_OVERRIDE')
ON CONFLICT(role_id,permission_id) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id,scope)
SELECT r.id,p.id,'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code = 'STAFF'
  AND p.code IN ('ORDER_HANDOVER_SHIPPING','ORDER_HIDE','ORDER_RESTORE','ORDER_CONTACT_CUSTOMER','ORDER_SUPPORT_CHAT')
ON CONFLICT(role_id,permission_id) DO NOTHING;

COMMIT;
