-- =========================================================
-- MONOLITH POSTGRESQL DATABASE
-- Consolidated from the previous service-specific schemas
-- =========================================================
-- Suggested database name: commerce_db
--
-- Mục tiêu:
--   1. Một PostgreSQL database duy nhất.
--   2. Giữ nguyên các bảng nghiệp vụ hiện tại.
--   3. Bổ sung FOREIGN KEY xuyên module ở cuối file.
--   4. Không thay đổi các giá trị status / enum / nghiệp vụ hiện có.
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- EXTENSIONS
-- =========================

-- =========================
-- USERS
-- =========================
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  username VARCHAR(50) UNIQUE NOT NULL,
  employee_code VARCHAR(30) UNIQUE,
  full_name VARCHAR(255),
  email VARCHAR(255) UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password_hash TEXT,

  date_of_birth DATE,
  gender VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
  avatar TEXT,

  job_title VARCHAR(150),
  position_id UUID,
  employment_type VARCHAR(30) CHECK (
    employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN', 'TEMPORARY')
  ),
  employment_status VARCHAR(30) DEFAULT 'ACTIVE' CHECK (
    employment_status IN ('ACTIVE', 'PROBATION', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED')
  ),
  hire_date DATE,
  termination_date DATE,
  work_location VARCHAR(150),
  manager_id UUID,

  active BOOLEAN DEFAULT TRUE,
  locked BOOLEAN DEFAULT FALSE,

  failed_login_attempts INT DEFAULT 0,
  login_locked_until TIMESTAMP,
  last_password_change TIMESTAMP,

  email_verified BOOLEAN DEFAULT FALSE,
  phone_verified BOOLEAN DEFAULT FALSE,

  last_login TIMESTAMP,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP,

  CONSTRAINT chk_users_employment_dates
    CHECK (termination_date IS NULL OR hire_date IS NULL OR termination_date >= hire_date),

  CONSTRAINT fk_users_manager
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_employment_status ON users(employment_status);

-- =========================
-- DEPARTMENTS
-- =========================
CREATE TABLE departments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(150) NOT NULL,
  description TEXT,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

-- Vị trí/chức danh chuẩn thuộc một phòng ban; dùng chung cho nhân sự và tuyển dụng.
CREATE TABLE positions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  department_id UUID NOT NULL,
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(150) NOT NULL,
  description TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,

  CONSTRAINT fk_positions_department
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
);

CREATE INDEX idx_positions_department_id ON positions(department_id);
CREATE INDEX idx_positions_active ON positions(active);

ALTER TABLE users
  ADD CONSTRAINT fk_users_position
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE SET NULL;

CREATE INDEX idx_users_position_id ON users(position_id);

-- Một nhân viên có thể thuộc nhiều phòng ban và ngược lại.
CREATE TABLE user_departments (
  user_id UUID,
  customer_id UUID,
  department_id UUID NOT NULL,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (user_id, department_id),

  CONSTRAINT fk_user_departments_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

  CONSTRAINT fk_user_departments_department
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_departments_department_id
  ON user_departments(department_id);

-- =========================
-- ROLES
-- =========================
CREATE TABLE roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- MODULES
-- =========================
CREATE TABLE modules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  icon VARCHAR(100),
  sort_order INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- PERMISSION GROUPS
-- =========================
CREATE TABLE permission_groups (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  module_id UUID NOT NULL,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_permission_groups_module
    FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE RESTRICT
);

CREATE INDEX idx_permission_groups_module_id ON permission_groups(module_id);

-- =========================
-- PERMISSIONS
-- =========================
CREATE TABLE permissions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(150) NOT NULL,
  code VARCHAR(100) UNIQUE NOT NULL,
  group_id UUID NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_permissions_group
    FOREIGN KEY (group_id)
    REFERENCES permission_groups(id)
    ON DELETE RESTRICT
);

-- =========================
-- USER_ROLES
-- =========================
CREATE TABLE user_roles (
  user_id UUID NOT NULL,
  role_id UUID NOT NULL,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (user_id, role_id),

  CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id)
    REFERENCES roles(id)
    ON DELETE CASCADE
);

-- =========================
-- ROLE PERMISSIONS
-- =========================
CREATE TABLE role_permissions (
  role_id UUID NOT NULL,
  permission_id UUID NOT NULL,
  scope VARCHAR(20) NOT NULL DEFAULT 'ALL',

  PRIMARY KEY (role_id, permission_id),

  CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id)
    REFERENCES roles(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id)
    REFERENCES permissions(id)
    ON DELETE CASCADE,

  CONSTRAINT chk_role_permissions_scope
    CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'STORE', 'ALL'))
);

-- =========================
-- USER PERMISSIONS
-- =========================
CREATE TABLE user_permissions (
  user_id UUID NOT NULL,
  permission_id UUID NOT NULL,
  effect VARCHAR(10) NOT NULL DEFAULT 'ALLOW',
  scope VARCHAR(20) NOT NULL DEFAULT 'ALL',

  PRIMARY KEY (user_id, permission_id),

  CONSTRAINT fk_user_permissions_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_user_permissions_permission
    FOREIGN KEY (permission_id)
    REFERENCES permissions(id)
    ON DELETE CASCADE,

  CONSTRAINT chk_user_permissions_effect
    CHECK (effect IN ('ALLOW', 'DENY')),

  CONSTRAINT chk_user_permissions_scope
    CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'STORE', 'ALL'))
);

-- =========================
-- USER TOKENS
-- =========================
CREATE TABLE user_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL,

  token_hash TEXT NOT NULL,
  token_type VARCHAR(30) DEFAULT 'REFRESH',

  refresh_token_family UUID,
  parent_token_id UUID,

  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP,

  device VARCHAR(100),
  ip_address VARCHAR(50),
  user_agent TEXT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_user_tokens_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_user_tokens_parent
    FOREIGN KEY (parent_token_id)
    REFERENCES user_tokens(id)
    ON DELETE SET NULL,

  CONSTRAINT chk_user_tokens_single_account
    CHECK ((user_id IS NOT NULL) <> (customer_id IS NOT NULL))
);

-- =========================
-- ROLE PERMISSION AUDIT
-- =========================
CREATE TABLE role_permission_audit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  role_id UUID,
  permission_id UUID,

  action VARCHAR(20) NOT NULL,
  changed_by UUID,

  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_audit_role
    FOREIGN KEY (role_id)
    REFERENCES roles(id)
    ON DELETE SET NULL,

  CONSTRAINT fk_audit_permission
    FOREIGN KEY (permission_id)
    REFERENCES permissions(id)
    ON DELETE SET NULL,

  CONSTRAINT fk_audit_user
    FOREIGN KEY (changed_by)
    REFERENCES users(id)
    ON DELETE SET NULL
);

-- =========================
-- AUTH AUDIT LOGS
-- =========================
CREATE TABLE auth_audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID,
  action VARCHAR(50) NOT NULL,
  description TEXT,

  ip_address VARCHAR(50),
  user_agent TEXT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_auth_logs_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE SET NULL
);

-- Audit nghiệp vụ append-only; JSONB giữ snapshot trước/sau và field thay đổi.
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID UNIQUE,
  actor_type VARCHAR(16) CHECK (actor_type IN ('EMPLOYEE', 'SYSTEM')),
  actor_user_id UUID,
  username VARCHAR(50) NOT NULL,
  action VARCHAR(40) NOT NULL CHECK (action ~ '^[A-Z][A-Z0-9_]{1,39}$'),
  request_id VARCHAR(128),
  method VARCHAR(16),
  path TEXT,
  job_name VARCHAR(128),
  row_count INTEGER CHECK (row_count >= 0),
  changes JSONB,
  occurred_at TIMESTAMPTZ,
  entity_type VARCHAR(100),
  entity_id UUID,
  old_data JSONB,
  new_data JSONB,
  changed_fields JSONB DEFAULT '[]'::jsonb,
  ip_address VARCHAR(64),
  user_agent TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_changes_gin ON audit_logs USING GIN (changes);
CREATE INDEX idx_audit_logs_actor_action_occurred ON audit_logs(actor_user_id, action, occurred_at DESC);
CREATE INDEX idx_audit_logs_request_id ON audit_logs(request_id);

CREATE TABLE audit_outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL UNIQUE,
  payload JSONB NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  file_appended_at TIMESTAMPTZ,
  kafka_published_at TIMESTAMPTZ,
  attempt_count INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ,
  last_error TEXT
);

CREATE INDEX idx_audit_outbox_pending
  ON audit_outbox(next_attempt_at, occurred_at, event_id)
  WHERE file_appended_at IS NULL OR kafka_published_at IS NULL;

CREATE OR REPLACE FUNCTION reject_audit_log_mutation() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'audit_logs is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_append_only
  BEFORE UPDATE OR DELETE ON audit_logs
  FOR EACH ROW EXECUTE FUNCTION reject_audit_log_mutation();

-- =========================
-- INDEXES
-- =========================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_user_tokens_user_id ON user_tokens(user_id);
CREATE UNIQUE INDEX uq_user_tokens_token_hash ON user_tokens(token_hash);

-- JWT đã bị thu hồi khi logout; account_id có thể là user hoặc customer.
CREATE TABLE revoked_tokens (
  token_id UUID PRIMARY KEY,
  account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('USER', 'CUSTOMER')),
  account_id UUID NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);
CREATE INDEX idx_user_tokens_customer_id ON user_tokens(customer_id);
CREATE INDEX idx_user_tokens_family ON user_tokens(refresh_token_family);
CREATE INDEX idx_permissions_group_id ON permissions(group_id);


-- =========================================================
-- V1__init_chat_service.sql
-- Chat Service Database Migration
-- =========================================================

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

CREATE TABLE customer_tiers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(100) NOT NULL,

  min_total_spent DECIMAL(14,2) DEFAULT 0 CHECK (min_total_spent >= 0),
  discount_percent DECIMAL(5,2) DEFAULT 0 
    CHECK (discount_percent >= 0 AND discount_percent <= 100),

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);


CREATE TABLE customers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password_hash TEXT,

  active BOOLEAN DEFAULT TRUE,
  locked BOOLEAN DEFAULT FALSE,

  full_name VARCHAR(255),
  date_of_birth DATE,

  gender VARCHAR(20) CHECK (
    gender IN ('MALE', 'FEMALE', 'OTHER')
  ),

  avatar TEXT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

ALTER TABLE user_tokens
  ADD CONSTRAINT fk_user_tokens_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;

CREATE TABLE customer_social_accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL,
  provider VARCHAR(20) NOT NULL CHECK (provider = 'GOOGLE'),
  provider_user_id VARCHAR(255) NOT NULL,
  provider_email VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP,

  CONSTRAINT uq_customer_social_provider_subject
    UNIQUE (provider, provider_user_id),
  CONSTRAINT uq_customer_social_customer_provider
    UNIQUE (customer_id, provider),
  CONSTRAINT fk_customer_social_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_social_customer_id
  ON customer_social_accounts(customer_id);

-- OTP quên mật khẩu dùng chung cho employee/customer. OTP và reset token chỉ lưu dạng hash.
CREATE TABLE password_reset_otps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('EMPLOYEE', 'CUSTOMER')),
  account_id UUID NOT NULL,
  email VARCHAR(255) NOT NULL,
  otp_hash TEXT NOT NULL,
  reset_token_hash VARCHAR(64),
  expires_at TIMESTAMP NOT NULL,
  reset_token_expires_at TIMESTAMP,
  verified_at TIMESTAMP,
  used_at TIMESTAMP,
  failed_attempts INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_otps_lookup
  ON password_reset_otps(email, account_type, created_at DESC);
CREATE UNIQUE INDEX uq_password_reset_otps_reset_token
  ON password_reset_otps(reset_token_hash) WHERE reset_token_hash IS NOT NULL;


CREATE TABLE customer_tier_assignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  customer_id UUID NOT NULL,
  tier_id UUID NOT NULL,

  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,

  note TEXT,

  CONSTRAINT fk_customer_tier_assignments_tier
    FOREIGN KEY (tier_id) REFERENCES customer_tiers(id)
);


CREATE UNIQUE INDEX uniq_active_tier_per_customer
ON customer_tier_assignments(customer_id)
WHERE expires_at IS NULL;


CREATE TABLE customer_addresses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  customer_id UUID NOT NULL,

  receiver_name VARCHAR(255) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,

  province VARCHAR(100),
  district VARCHAR(100),
  ward VARCHAR(100),

  address_line TEXT NOT NULL,
  postal_code VARCHAR(20),

      -- tọa độ giao hàng
  latitude  DECIMAL(9,6) CHECK (latitude BETWEEN -90 AND 90),
  longitude DECIMAL(9,6) CHECK (longitude BETWEEN -180 AND 180),

  is_default BOOLEAN DEFAULT FALSE,

  address_type VARCHAR(30) DEFAULT 'HOME' CHECK (
    address_type IN ('HOME', 'WORK', 'OTHER')
  ),

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);


CREATE UNIQUE INDEX uniq_default_address_per_customer
ON customer_addresses(customer_id)
WHERE is_default = TRUE;


CREATE TABLE loyalty_accounts (
  customer_id UUID PRIMARY KEY,

  total_spent DECIMAL(14,2) DEFAULT 0 
    CHECK (total_spent >= 0),

  points_balance INT DEFAULT 0 
    CHECK (points_balance >= 0),

  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE loyalty_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  customer_id UUID NOT NULL,

  transaction_type VARCHAR(30) NOT NULL CHECK (
    transaction_type IN ('EARN', 'REDEEM', 'EXPIRE', 'ADJUST')
  ),

  points INT NOT NULL CHECK (points <> 0),

  reference_type VARCHAR(50),
  reference_id UUID,

  note TEXT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE customer_activity_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  customer_id UUID NOT NULL,

  action VARCHAR(50) NOT NULL CHECK (
    action IN (
      'UPDATE_PROFILE',
      'ADD_ADDRESS',
      'UPDATE_ADDRESS',
      'DELETE_ADDRESS',
      'SET_DEFAULT_ADDRESS',
      'CHANGE_TIER',
      'EARN_POINTS',
      'REDEEM_POINTS'
    )
  ),

  entity_type VARCHAR(30) NOT NULL CHECK (
    entity_type IN ('PROFILE', 'ADDRESS', 'TIER', 'LOYALTY')
  ),

  entity_id UUID,

  metadata JSONB,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- addresses
CREATE INDEX idx_customer_addresses_customer_id
ON customer_addresses(customer_id);

CREATE INDEX idx_customer_addresses_default 
ON customer_addresses(customer_id, is_default);

-- tiers
CREATE INDEX idx_customer_tier_assignments_customer_id
ON customer_tier_assignments(customer_id);

CREATE INDEX idx_customer_tier_assignments_tier_id
ON customer_tier_assignments(tier_id);

-- loyalty
CREATE INDEX idx_loyalty_transactions_customer_id
ON loyalty_transactions(customer_id);

CREATE INDEX idx_loyalty_transactions_type 
ON loyalty_transactions(transaction_type);

-- activity logs
CREATE INDEX idx_customer_activity_customer
ON customer_activity_logs(customer_id);

CREATE INDEX idx_customer_activity_entity 
ON customer_activity_logs(entity_type, entity_id);

CREATE INDEX idx_customer_activity_created_at 
ON customer_activity_logs(created_at);


-- =========================================================
-- INVENTORY SERVICE SCHEMA
-- =========================================================

-- =========================================================
-- STORES
-- =========================================================

CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    phone VARCHAR(20) NOT NULL,

    -- tọa độ cửa hàng
    latitude  DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,

    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_stores_coordinates UNIQUE (latitude, longitude)
);

-- =========================================================
-- STORE STAFFS
-- =========================================================

CREATE TABLE store_staffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    store_id UUID NOT NULL,
    staff_role VARCHAR(50),
    start_date DATE,
    end_date DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_staff_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- SUPPLIERS
-- =========================================================

CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255),
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    address TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE SEQUENCE supplier_code_seq;

-- =========================================================
-- GOODS RECEIPTS
-- =========================================================

CREATE TABLE goods_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_code VARCHAR(50) UNIQUE NOT NULL,
    supplier_id UUID,
    store_id UUID NOT NULL,
    received_by UUID,
    approved_by UUID,
    receipt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    total_quantity INT DEFAULT 0,
    total_amount DECIMAL(14,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_goods_receipt_supplier
        FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id),

    CONSTRAINT fk_goods_receipt_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- GOODS RECEIPT ITEMS
-- =========================================================

CREATE TABLE goods_receipt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(100),
    product_name VARCHAR(255),
    cost_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    total DECIMAL(14,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_goods_receipt_item_receipt
        FOREIGN KEY (receipt_id)
        REFERENCES goods_receipts(id)
        ON DELETE CASCADE
);

-- =========================================================
-- GOODS ISSUES
-- =========================================================

CREATE TABLE goods_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_code VARCHAR(50) UNIQUE NOT NULL,
    store_id UUID NOT NULL,
    order_id UUID,
    issued_by UUID,
    approved_by UUID,
    issue_type VARCHAR(50) NOT NULL,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    total_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_goods_issue_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- GOODS ISSUE ITEMS
-- =========================================================

CREATE TABLE goods_issue_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(100),
    product_name VARCHAR(255),
    quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_goods_issue_item_issue
        FOREIGN KEY (issue_id)
        REFERENCES goods_issues(id)
        ON DELETE CASCADE
);

-- =========================================================
-- INVENTORY TRANSACTIONS
-- =========================================================

CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_variant_id UUID NOT NULL,
    store_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    quantity INT NOT NULL,
    balance_after INT NOT NULL,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_transaction_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- INVENTORY BALANCES
-- =========================================================

CREATE TABLE inventory_balances (
    store_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    damaged_quantity INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (store_id, product_variant_id),

    CONSTRAINT fk_inventory_balance_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- STOCK RESERVATIONS
-- =========================================================

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    store_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    expired_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_stock_reservation_store
        FOREIGN KEY (store_id)
        REFERENCES stores(id)
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_store_staffs_store_id
    ON store_staffs(store_id);

CREATE INDEX idx_store_staffs_user_id
    ON store_staffs(user_id);

CREATE INDEX idx_goods_receipts_store_id
    ON goods_receipts(store_id);

CREATE INDEX idx_goods_receipts_supplier_id
    ON goods_receipts(supplier_id);

CREATE INDEX idx_goods_receipt_items_receipt_id
    ON goods_receipt_items(receipt_id);

CREATE INDEX idx_goods_receipt_items_variant_id
    ON goods_receipt_items(product_variant_id);

CREATE INDEX idx_goods_issues_store_id
    ON goods_issues(store_id);

CREATE INDEX idx_goods_issues_order_id
    ON goods_issues(order_id);

CREATE INDEX idx_goods_issue_items_issue_id
    ON goods_issue_items(issue_id);

CREATE INDEX idx_inventory_transactions_variant_store
    ON inventory_transactions(product_variant_id, store_id);

CREATE INDEX idx_stock_reservations_order_id
    ON stock_reservations(order_id);

-- =========================================================
-- V1__init_notification_service.sql
-- Notification Service Database Migration
-- =========================================================

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

/* =========================================================
   ORDER SERVICE DATABASE SCHEMA
   Database: order_db
   ========================================================= */

/* =========================================================
   ORDERS
   ========================================================= */

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_code VARCHAR(50) UNIQUE NOT NULL,

    customer_id UUID,

    store_id UUID,

    order_type VARCHAR(20) NOT NULL,

    status VARCHAR(50) NOT NULL,

    subtotal DECIMAL(14,2) DEFAULT 0,

    discount_total DECIMAL(14,2) DEFAULT 0,

    tax DECIMAL(14,2) DEFAULT 0,

    shipping_fee DECIMAL(14,2) DEFAULT 0,

    total_amount DECIMAL(14,2) DEFAULT 0,

    payment_status VARCHAR(50) DEFAULT 'UNPAID',

    note TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

/* =========================================================
   ORDER ITEMS
   ========================================================= */

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    product_id UUID,

    product_variant_id UUID NOT NULL,

    product_name VARCHAR(255) NOT NULL,

    sku VARCHAR(100),

    color VARCHAR(100),

    size VARCHAR(50),

    image_url TEXT,

    price DECIMAL(12,2) NOT NULL,

    quantity INT NOT NULL,

    total DECIMAL(14,2) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   ORDER ADDRESSES
   ========================================================= */

CREATE TABLE order_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,

    receiver_name VARCHAR(255) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,

    province VARCHAR(100),
    district VARCHAR(100),
    ward VARCHAR(100),
    address_line TEXT,
    postal_code VARCHAR(20),

    -- tọa độ giao hàng
    latitude  DECIMAL(9,6) CHECK (latitude BETWEEN -90 AND 90),
    longitude DECIMAL(9,6) CHECK (longitude BETWEEN -180 AND 180),

    address_type VARCHAR(20) DEFAULT 'SHIPPING',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   ORDER STATUS HISTORIES
   ========================================================= */

CREATE TABLE order_status_histories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    from_status VARCHAR(50),

    to_status VARCHAR(50) NOT NULL,

    changed_by UUID,

    note TEXT,

    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   SHIPMENTS
   ========================================================= */

CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,

    shipping_provider VARCHAR(100),

    tracking_code VARCHAR(100),

    shipping_status VARCHAR(50) DEFAULT 'PENDING',

    shipped_at TIMESTAMP,

    delivered_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

/* =========================================================
   ORDER PROMOTIONS
   ========================================================= */

CREATE TABLE order_promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    promotion_id UUID NOT NULL,

    promotion_code VARCHAR(50),

    discount_amount DECIMAL(14,2) DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

/* =========================================================
   FOREIGN KEYS
   ========================================================= */

ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE order_addresses
ADD CONSTRAINT fk_order_addresses_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE order_status_histories
ADD CONSTRAINT fk_order_status_histories_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE shipments
ADD CONSTRAINT fk_shipments_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

ALTER TABLE order_promotions
ADD CONSTRAINT fk_order_promotions_order
FOREIGN KEY (order_id)
REFERENCES orders(id)
ON DELETE CASCADE;

/* =========================================================
   INDEXES
   ========================================================= */

CREATE INDEX idx_orders_customer_id
ON orders(customer_id);

CREATE INDEX idx_orders_store_id
ON orders(store_id);

CREATE INDEX idx_orders_status
ON orders(status);

CREATE INDEX idx_order_items_order_id
ON order_items(order_id);

CREATE INDEX idx_order_items_variant_id
ON order_items(product_variant_id);

CREATE INDEX idx_order_status_histories_order_id
ON order_status_histories(order_id);

CREATE INDEX idx_shipments_order_id
ON shipments(order_id);

CREATE INDEX idx_order_promotions_order_id
ON order_promotions(order_id);

/* =========================================================
   CHECK CONSTRAINTS
   ========================================================= */

ALTER TABLE order_items
ADD CONSTRAINT chk_order_items_quantity
CHECK (quantity > 0);

ALTER TABLE order_items
ADD CONSTRAINT chk_order_items_price
CHECK (price >= 0);

ALTER TABLE orders
ADD CONSTRAINT chk_orders_total_amount
CHECK (total_amount >= 0);

/* =========================================================
   COMMENTS
   ========================================================= */

COMMENT ON TABLE orders IS 'Stores customer orders';

COMMENT ON TABLE order_items IS 'Stores items belonging to an order';

COMMENT ON TABLE order_addresses IS 'Stores shipping address of an order';

COMMENT ON TABLE order_status_histories IS 'Stores order status change history';

COMMENT ON TABLE shipments IS 'Stores shipment information';

COMMENT ON TABLE order_promotions IS 'Stores promotions applied to orders';

-- =========================================================
-- V1__init_payment_service.sql
-- Payment Service Database Migration
-- =========================================================

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

/* =========================================================
   PRODUCT SERVICE - INITIAL SCHEMA
   ========================================================= */

-- Enable UUID generation

-- =========================
-- 1. brands
-- =========================
CREATE TABLE brands (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  code VARCHAR(100) UNIQUE,
  logo TEXT,
  description TEXT,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  terminated_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

-- =========================
-- 2. collections
-- =========================
CREATE TABLE collections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  brand_id UUID,
  name VARCHAR(255) NOT NULL,
  code VARCHAR(100) UNIQUE,
  season VARCHAR(50),
  year INT,
  release_date DATE,
  description TEXT,
  image_url TEXT,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT fk_collections_brand
    FOREIGN KEY (brand_id) REFERENCES brands(id)
);

-- =========================
-- 3. categories
-- =========================
CREATE TABLE categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  parent_id UUID,
  name VARCHAR(255) NOT NULL,
  code VARCHAR(100) UNIQUE,
  image_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT fk_categories_parent
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- =========================
-- 4. products
-- =========================
CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  brand_id UUID,
  collection_id UUID,
  category_id UUID,
  name VARCHAR(255) NOT NULL,
  slug VARCHAR(255) UNIQUE,
  description TEXT,
  material VARCHAR(255),
  fit VARCHAR(100),
  gender VARCHAR(20),
  status VARCHAR(50) DEFAULT 'DRAFT',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  image_url TEXT NOT NULL,
  CONSTRAINT fk_products_brand
    FOREIGN KEY (brand_id) REFERENCES brands(id),
  CONSTRAINT fk_products_collection
    FOREIGN KEY (collection_id) REFERENCES collections(id),
  CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- =========================
-- 5. product_variants
-- =========================
CREATE TABLE product_variants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL,
  sku VARCHAR(100) UNIQUE NOT NULL,
  color VARCHAR(100),
  size VARCHAR(50),
  price DECIMAL(12,2) NOT NULL,
  sale_price DECIMAL(12,2),
  weight DECIMAL(8,2),
  barcode VARCHAR(100),
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT fk_variants_product
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =========================
-- 6. product_images
-- =========================
CREATE TABLE product_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_variant_id UUID NOT NULL,
  image_url TEXT NOT NULL,
  cloudinary_public_id VARCHAR(255),
  is_primary BOOLEAN DEFAULT FALSE,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_images_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

-- =========================
-- 7. product_attributes
-- =========================
CREATE TABLE product_attributes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL,
  attribute_name VARCHAR(100) NOT NULL,
  attribute_value VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attributes_product
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =========================
-- 8. product_tags
-- =========================
CREATE TABLE product_tags (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 9. product_tag_mappings
-- =========================
CREATE TABLE product_tag_mappings (
  product_id UUID NOT NULL,
  tag_id UUID NOT NULL,
  PRIMARY KEY (product_id, tag_id),
  CONSTRAINT fk_tagmap_product
    FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_tagmap_tag
    FOREIGN KEY (tag_id) REFERENCES product_tags(id)
);

-- =========================
-- INDEXES
-- =========================
CREATE INDEX idx_collections_brand_id ON collections(brand_id);
CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_collection_id ON products(collection_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_product_images_variant_id ON product_images(product_variant_id);
CREATE INDEX idx_product_images_cloudinary_public_id ON product_images(cloudinary_public_id);
CREATE INDEX idx_product_attributes_product_id ON product_attributes(product_id);

-- =========================================================
-- Promotion Service Database Migration
-- File: V1__init_promotion_service.sql
-- =========================================================

-- =========================================================
-- ENUMS
-- =========================================================

CREATE TYPE discount_type_enum AS ENUM (
    'PERCENT',
    'FIXED'
);

-- =========================================================
-- TABLE: promotions
-- =========================================================

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(255) NOT NULL,

    discount_type discount_type_enum NOT NULL,

    discount_value DECIMAL(10,2) NOT NULL,

    start_date TIMESTAMP,

    end_date TIMESTAMP,

    min_order_value DECIMAL(14,2),

    max_discount DECIMAL(14,2),

    usage_limit INT,

    usage_per_user INT,

    active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP
);

-- =========================================================
-- TABLE: promotion_tiers
-- =========================================================

CREATE TABLE promotion_tiers (
    promotion_id UUID NOT NULL,
    tier_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, tier_id),

    CONSTRAINT fk_promotion_tiers_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_products
-- =========================================================

CREATE TABLE promotion_products (
    promotion_id UUID NOT NULL,
    product_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, product_id),

    CONSTRAINT fk_promotion_products_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_categories
-- =========================================================

CREATE TABLE promotion_categories (
    promotion_id UUID NOT NULL,
    category_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, category_id),

    CONSTRAINT fk_promotion_categories_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_brands
-- =========================================================

CREATE TABLE promotion_brands (
    promotion_id UUID NOT NULL,
    brand_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, brand_id),

    CONSTRAINT fk_promotion_brands_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_collections
-- =========================================================

CREATE TABLE promotion_collections (
    promotion_id UUID NOT NULL,
    collection_id UUID NOT NULL,

    PRIMARY KEY (promotion_id, collection_id),

    CONSTRAINT fk_promotion_collections_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_usages
-- =========================================================

CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    promotion_id UUID NOT NULL,

    order_id UUID NOT NULL,

    customer_id UUID,

    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promotion_usages_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- TABLE: promotion_conditions
-- =========================================================

CREATE TABLE promotion_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    promotion_id UUID NOT NULL,

    condition_type VARCHAR(50) NOT NULL,

    condition_value VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promotion_conditions_promotion
        FOREIGN KEY (promotion_id)
        REFERENCES promotions(id)
        ON DELETE CASCADE
);

-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_promotions_code
    ON promotions(code);

CREATE INDEX idx_promotions_active
    ON promotions(active);

CREATE INDEX idx_promotions_start_date
    ON promotions(start_date);

CREATE INDEX idx_promotions_end_date
    ON promotions(end_date);

CREATE INDEX idx_promotion_usages_promotion_id
    ON promotion_usages(promotion_id);

CREATE INDEX idx_promotion_usages_order_id
    ON promotion_usages(order_id);

CREATE INDEX idx_promotion_usages_customer_id
    ON promotion_usages(customer_id);

CREATE INDEX idx_promotion_conditions_promotion_id
    ON promotion_conditions(promotion_id);

-- =========================================================
-- COMMENTS
-- =========================================================

COMMENT ON TABLE promotions IS 'Main promotion table';

COMMENT ON TABLE promotion_tiers IS
'Mapping promotion with membership tiers';

COMMENT ON TABLE promotion_products IS
'Mapping promotion with products';

COMMENT ON TABLE promotion_categories IS
'Mapping promotion with categories';

COMMENT ON TABLE promotion_brands IS
'Mapping promotion with brands';

COMMENT ON TABLE promotion_collections IS
'Mapping promotion with collections';

COMMENT ON TABLE promotion_usages IS
'Promotion usage history';

COMMENT ON TABLE promotion_conditions IS
'Dynamic promotion conditions';

-- =========================================================
-- CROSS-MODULE FOREIGN KEYS
-- Các quan hệ trước đây chỉ là UUID giữa các service,
-- nay được ràng buộc trực tiếp vì toàn bộ dữ liệu nằm chung DB.
-- Không chỉ định ON DELETE để tránh tự ý thay đổi nghiệp vụ hiện tại.
-- =========================================================

-- ---------------------------------------------------------
-- CUSTOMER DOMAIN (độc lập hoàn toàn với users/nhân viên)
-- ---------------------------------------------------------
ALTER TABLE customer_tier_assignments
    ADD CONSTRAINT fk_customer_tier_assignments_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE customer_addresses
    ADD CONSTRAINT fk_customer_addresses_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE loyalty_accounts
    ADD CONSTRAINT fk_loyalty_accounts_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE loyalty_transactions
    ADD CONSTRAINT fk_loyalty_transactions_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE customer_activity_logs
    ADD CONSTRAINT fk_customer_activity_logs_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);


-- ---------------------------------------------------------
-- CHAT <-> AUTH / ORDER
-- sender_id và recipient kiểu polymorphic được giữ nguyên,
-- không ép FK nếu có thể đại diện cho nhiều loại actor.
-- ---------------------------------------------------------
ALTER TABLE order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE order_chat_rooms
    ADD CONSTRAINT fk_order_chat_rooms_staff
    FOREIGN KEY (assigned_staff_id) REFERENCES users(id);

ALTER TABLE order_chat_message_status
    ADD CONSTRAINT fk_order_chat_message_status_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE internal_chat_rooms
    ADD CONSTRAINT fk_internal_chat_rooms_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);


-- ---------------------------------------------------------
-- STORE / INVENTORY <-> AUTH / PRODUCT / ORDER
-- ---------------------------------------------------------
ALTER TABLE store_staffs
    ADD CONSTRAINT fk_store_staffs_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE goods_receipts
    ADD CONSTRAINT fk_goods_receipts_received_by
    FOREIGN KEY (received_by) REFERENCES users(id);

ALTER TABLE goods_receipts
    ADD CONSTRAINT fk_goods_receipts_approved_by
    FOREIGN KEY (approved_by) REFERENCES users(id);

ALTER TABLE goods_receipt_items
    ADD CONSTRAINT fk_goods_receipt_items_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

ALTER TABLE goods_issues
    ADD CONSTRAINT fk_goods_issues_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE goods_issues
    ADD CONSTRAINT fk_goods_issues_issued_by
    FOREIGN KEY (issued_by) REFERENCES users(id);

ALTER TABLE goods_issues
    ADD CONSTRAINT fk_goods_issues_approved_by
    FOREIGN KEY (approved_by) REFERENCES users(id);

ALTER TABLE goods_issue_items
    ADD CONSTRAINT fk_goods_issue_items_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

ALTER TABLE inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

ALTER TABLE inventory_transactions
    ADD CONSTRAINT fk_inventory_transactions_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE inventory_balances
    ADD CONSTRAINT fk_inventory_balances_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

ALTER TABLE stock_reservations
    ADD CONSTRAINT fk_stock_reservations_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE stock_reservations
    ADD CONSTRAINT fk_stock_reservations_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);


-- ---------------------------------------------------------
-- NOTIFICATION <-> AUTH
-- notifications.recipient_id vẫn polymorphic nên không FK.
-- ---------------------------------------------------------
ALTER TABLE user_notification_preferences
    ADD CONSTRAINT fk_user_notification_preferences_user
    FOREIGN KEY (user_id) REFERENCES users(id);


-- ---------------------------------------------------------
-- ORDER <-> AUTH / STORE / PRODUCT / PROMOTION
-- ---------------------------------------------------------
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_store
    FOREIGN KEY (store_id) REFERENCES stores(id);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

ALTER TABLE order_status_histories
    ADD CONSTRAINT fk_order_status_histories_changed_by
    FOREIGN KEY (changed_by) REFERENCES users(id);

ALTER TABLE order_promotions
    ADD CONSTRAINT fk_order_promotions_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions(id);


-- ---------------------------------------------------------
-- PAYMENT <-> ORDER / AUTH
-- ---------------------------------------------------------
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE refunds
    ADD CONSTRAINT fk_refunds_requested_by
    FOREIGN KEY (requested_by) REFERENCES users(id);


-- ---------------------------------------------------------
-- PROMOTION <-> CUSTOMER / PRODUCT / ORDER / AUTH
-- ---------------------------------------------------------
ALTER TABLE promotion_tiers
    ADD CONSTRAINT fk_promotion_tiers_tier
    FOREIGN KEY (tier_id) REFERENCES customer_tiers(id);

ALTER TABLE promotion_products
    ADD CONSTRAINT fk_promotion_products_product
    FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE promotion_categories
    ADD CONSTRAINT fk_promotion_categories_category
    FOREIGN KEY (category_id) REFERENCES categories(id);

ALTER TABLE promotion_brands
    ADD CONSTRAINT fk_promotion_brands_brand
    FOREIGN KEY (brand_id) REFERENCES brands(id);

ALTER TABLE promotion_collections
    ADD CONSTRAINT fk_promotion_collections_collection
    FOREIGN KEY (collection_id) REFERENCES collections(id);

ALTER TABLE promotion_usages
    ADD CONSTRAINT fk_promotion_usages_order
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE promotion_usages
    ADD CONSTRAINT fk_promotion_usages_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);


-- =========================================================
-- ADDITIONAL INDEXES FOR CROSS-MODULE RELATIONS
-- =========================================================
CREATE INDEX IF NOT EXISTS idx_order_chat_rooms_customer_id
    ON order_chat_rooms(customer_id);

CREATE INDEX IF NOT EXISTS idx_order_chat_rooms_staff_id
    ON order_chat_rooms(assigned_staff_id);

CREATE INDEX IF NOT EXISTS idx_orders_customer_store
    ON orders(customer_id, store_id);

CREATE INDEX IF NOT EXISTS idx_payments_order_status
    ON payments(order_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_balances_variant
    ON inventory_balances(product_variant_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_variant
    ON stock_reservations(product_variant_id);

CREATE INDEX IF NOT EXISTS idx_promotion_tiers_tier
    ON promotion_tiers(tier_id);

CREATE INDEX IF NOT EXISTS idx_promotion_products_product
    ON promotion_products(product_id);

CREATE INDEX IF NOT EXISTS idx_promotion_categories_category
    ON promotion_categories(category_id);

CREATE INDEX IF NOT EXISTS idx_promotion_brands_brand
    ON promotion_brands(brand_id);

CREATE INDEX IF NOT EXISTS idx_promotion_collections_collection
    ON promotion_collections(collection_id);

-- =========================================================
-- END OF MONOLITH DATABASE SCHEMA
-- =========================================================
