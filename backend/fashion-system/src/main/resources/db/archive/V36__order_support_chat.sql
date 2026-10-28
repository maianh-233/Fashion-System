-- Order support chat: scripted bot (no AI) that hands the conversation to store
-- staff, admin-editable FAQ/policy answers, and the "Hỗ trợ đơn hàng" permission group.

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM order_chat_rooms) OR EXISTS (SELECT 1 FROM order_chat_messages) THEN
        RAISE EXCEPTION 'Support chat migration stopped: legacy chat rows exist';
    END IF;
END $$;

-- 1. Conversations belong to the order the customer sees (the parent of a split order).
--    customer_id pointed at users; customers live in their own table.
ALTER TABLE order_chat_rooms DROP CONSTRAINT IF EXISTS fk_order_chat_rooms_customer;
ALTER TABLE order_chat_rooms ADD CONSTRAINT fk_order_chat_rooms_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);
ALTER TABLE order_chat_rooms ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE order_chat_rooms ALTER COLUMN status SET DEFAULT 'BOT_HANDLING';
ALTER TABLE order_chat_rooms DROP CONSTRAINT IF EXISTS chk_order_chat_rooms_status;
ALTER TABLE order_chat_rooms ADD CONSTRAINT chk_order_chat_rooms_status
    CHECK (status IN ('BOT_HANDLING','WAITING_STAFF','STAFF_HANDLING','CLOSED'));
ALTER TABLE order_chat_rooms
    ADD COLUMN IF NOT EXISTS escalated_at timestamp,
    ADD COLUMN IF NOT EXISTS assigned_at timestamp,
    ADD COLUMN IF NOT EXISTS staff_unread_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS customer_unread_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS closed_by uuid;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_chat_rooms_closed_by') THEN
        ALTER TABLE order_chat_rooms ADD CONSTRAINT fk_order_chat_rooms_closed_by FOREIGN KEY (closed_by) REFERENCES users(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_order_chat_rooms_status ON order_chat_rooms(status, last_message_at DESC);

-- 2. Messages: CUSTOMER / BOT / STAFF / SYSTEM. metadata keeps the bot's quick-reply options.
ALTER TABLE order_chat_messages DROP CONSTRAINT IF EXISTS chk_order_chat_messages_sender;
ALTER TABLE order_chat_messages ADD CONSTRAINT chk_order_chat_messages_sender
    CHECK (sender_type IN ('CUSTOMER','BOT','STAFF','SYSTEM'));
CREATE INDEX IF NOT EXISTS idx_order_chat_messages_room ON order_chat_messages(room_id, created_at);

-- 3. Policy answers the bot reads; administrators edit the wording.
CREATE TABLE IF NOT EXISTS support_faqs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    topic varchar(30) NOT NULL,
    title varchar(200) NOT NULL,
    answer text NOT NULL,
    keywords varchar(500),
    sort_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    updated_by uuid REFERENCES users(id),
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_support_faqs_topic CHECK (topic IN ('RETURN','CANCEL','SHIPPING','PAYMENT'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_support_faqs_topic ON support_faqs(topic);

-- Starting text follows the rules implemented in the order module; the return policy is
-- a placeholder until the shop publishes its own.
INSERT INTO support_faqs(topic, title, answer, keywords, sort_order) VALUES
 ('CANCEL', 'Chính sách hủy đơn',
  'Bạn có thể tự hủy đơn khi cửa hàng chưa xác nhận và đơn chưa thanh toán. Sau khi cửa hàng đã xác nhận, vui lòng gặp nhân viên để được hỗ trợ. Đơn đã giao cho đơn vị vận chuyển thì không thể hủy. Voucher đã dùng cho đơn bị hủy sẽ không được hoàn lại.',
  'hủy,huỷ,huy,cancel', 1),
 ('SHIPPING', 'Chính sách vận chuyển',
  'Đơn được giao từ cửa hàng gần bạn nhất còn đủ hàng. Nếu một cửa hàng không đủ, đơn sẽ được chia thành nhiều phần giao từ các cửa hàng khác nhau và bạn sẽ được thông báo chi tiết từng phần. Bạn có thể theo dõi chuyến giao bằng đường link trong thông tin đơn hàng.',
  'giao,ship,vận chuyển,van chuyen,bao lâu,khi nào', 2),
 ('PAYMENT', 'Chính sách thanh toán',
  'Hiện tại cửa hàng hỗ trợ thanh toán khi nhận hàng (COD): bạn thanh toán cho tài xế khi nhận hàng. Thanh toán online qua VNPay và MoMo sẽ sớm được hỗ trợ.',
  'thanh toán,thanh toan,trả tiền,tra tien,cod,vnpay,momo,chuyển khoản', 3),
 ('RETURN', 'Chính sách đổi trả',
  'Cửa hàng đang cập nhật chính sách đổi trả. Vui lòng gặp nhân viên để được hỗ trợ đổi hoặc trả sản phẩm.',
  'đổi,trả,doi,tra,hoàn,hoan,lỗi,size', 4)
ON CONFLICT DO NOTHING;

-- 4. "Hỗ trợ đơn hàng" permission group (its own sidebar entry) in the SALES module.
INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:ORDER_SUPPORT'),1,8) || '-' || substr(md5('permission_group:ORDER_SUPPORT'),9,4) || '-' ||
        substr(md5('permission_group:ORDER_SUPPORT'),13,4) || '-' || substr(md5('permission_group:ORDER_SUPPORT'),17,4) || '-' ||
        substr(md5('permission_group:ORDER_SUPPORT'),21,12))::uuid,
       'Hỗ trợ đơn hàng', 'ORDER_SUPPORT', 'Hội thoại hỗ trợ khách về đơn hàng và câu trả lời của bot', current_timestamp, m.id
FROM modules m WHERE m.code = 'SALES'
ON CONFLICT (code) DO NOTHING;

UPDATE permissions SET group_id = (SELECT id FROM permission_groups WHERE code = 'ORDER_SUPPORT')
WHERE code = 'ORDER_SUPPORT_CHAT';

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:SUPPORT_FAQ_MANAGE'),1,8) || '-' || substr(md5('permission:SUPPORT_FAQ_MANAGE'),9,4) || '-' ||
        substr(md5('permission:SUPPORT_FAQ_MANAGE'),13,4) || '-' || substr(md5('permission:SUPPORT_FAQ_MANAGE'),17,4) || '-' ||
        substr(md5('permission:SUPPORT_FAQ_MANAGE'),21,12))::uuid,
       'SUPPORT_FAQ_MANAGE', 'Quản lý câu trả lời của bot', g.id,
       'Sửa nội dung chính sách mà bot hỗ trợ đơn hàng trả lời khách', current_timestamp
FROM permission_groups g WHERE g.code = 'ORDER_SUPPORT'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN') AND p.code = 'SUPPORT_FAQ_MANAGE'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
