BEGIN;

-- Nhân sự: một user có thể thuộc nhiều phòng ban.
CREATE TABLE departments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(150) NOT NULL,
  description TEXT,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE user_departments (
  user_id UUID NOT NULL,
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

-- Giữ nguyên UUID khách hàng để không mất quan hệ dữ liệu, nhưng loại bỏ FK tới users.
ALTER TABLE customer_profiles
  ADD COLUMN username VARCHAR(50),
  ADD COLUMN email VARCHAR(255),
  ADD COLUMN phone VARCHAR(20),
  ADD COLUMN password_hash TEXT,
  ADD COLUMN active BOOLEAN DEFAULT TRUE,
  ADD COLUMN locked BOOLEAN DEFAULT FALSE;

UPDATE customer_profiles customer
SET username = employee.username,
    email = employee.email,
    phone = employee.phone,
    password_hash = employee.password_hash,
    active = employee.active,
    locked = employee.locked
FROM users employee
WHERE employee.id = customer.user_id;

ALTER TABLE customer_profiles
  ALTER COLUMN username SET NOT NULL,
  ADD CONSTRAINT uq_customers_username UNIQUE (username),
  ADD CONSTRAINT uq_customers_email UNIQUE (email),
  ADD CONSTRAINT uq_customers_phone UNIQUE (phone),
  DROP CONSTRAINT IF EXISTS fk_customer_profiles_user;

ALTER TABLE customer_profiles RENAME TO customers;
ALTER TABLE customers RENAME COLUMN user_id TO id;

ALTER TABLE user_tiers DROP CONSTRAINT IF EXISTS fk_user_tiers_user;
ALTER TABLE user_tiers RENAME TO customer_tier_assignments;
ALTER TABLE customer_tier_assignments RENAME COLUMN user_id TO customer_id;
ALTER TABLE customer_tier_assignments
  ADD CONSTRAINT fk_customer_tier_assignments_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE customer_addresses DROP CONSTRAINT IF EXISTS fk_customer_addresses_user;
ALTER TABLE customer_addresses RENAME COLUMN user_id TO customer_id;
ALTER TABLE customer_addresses
  ADD CONSTRAINT fk_customer_addresses_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE loyalty_accounts DROP CONSTRAINT IF EXISTS fk_loyalty_accounts_user;
ALTER TABLE loyalty_accounts RENAME COLUMN user_id TO customer_id;
ALTER TABLE loyalty_accounts
  ADD CONSTRAINT fk_loyalty_accounts_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE loyalty_transactions DROP CONSTRAINT IF EXISTS fk_loyalty_transactions_user;
ALTER TABLE loyalty_transactions RENAME COLUMN user_id TO customer_id;
ALTER TABLE loyalty_transactions
  ADD CONSTRAINT fk_loyalty_transactions_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE customer_activity_logs DROP CONSTRAINT IF EXISTS fk_customer_activity_logs_user;
ALTER TABLE customer_activity_logs RENAME COLUMN user_id TO customer_id;
ALTER TABLE customer_activity_logs
  ADD CONSTRAINT fk_customer_activity_logs_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE order_chat_rooms DROP CONSTRAINT IF EXISTS fk_order_chat_rooms_customer;
ALTER TABLE order_chat_rooms
  ADD CONSTRAINT fk_order_chat_rooms_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_user;
ALTER TABLE orders RENAME COLUMN user_id TO customer_id;
ALTER TABLE orders
  ADD CONSTRAINT fk_orders_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE promotion_usages DROP CONSTRAINT IF EXISTS fk_promotion_usages_user;
ALTER TABLE promotion_usages RENAME COLUMN user_id TO customer_id;
ALTER TABLE promotion_usages
  ADD CONSTRAINT fk_promotion_usages_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER INDEX IF EXISTS uniq_active_tier_per_user RENAME TO uniq_active_tier_per_customer;
ALTER INDEX IF EXISTS uniq_default_address_per_user RENAME TO uniq_default_address_per_customer;
ALTER INDEX IF EXISTS idx_customer_addresses_user_id RENAME TO idx_customer_addresses_customer_id;
ALTER INDEX IF EXISTS idx_user_tiers_user_id RENAME TO idx_customer_tier_assignments_customer_id;
ALTER INDEX IF EXISTS idx_user_tiers_tier_id RENAME TO idx_customer_tier_assignments_tier_id;
ALTER INDEX IF EXISTS idx_loyalty_transactions_user_id RENAME TO idx_loyalty_transactions_customer_id;
ALTER INDEX IF EXISTS idx_customer_activity_user RENAME TO idx_customer_activity_customer;
ALTER INDEX IF EXISTS idx_orders_user_id RENAME TO idx_orders_customer_id;
ALTER INDEX IF EXISTS idx_promotion_usages_user_id RENAME TO idx_promotion_usages_customer_id;

COMMIT;
