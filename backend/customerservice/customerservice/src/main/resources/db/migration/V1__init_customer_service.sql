CREATE EXTENSION IF NOT EXISTS pgcrypto;


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


CREATE TABLE customer_profiles (
  user_id UUID PRIMARY KEY,

  full_name VARCHAR(255),
  date_of_birth DATE,

  gender VARCHAR(20) CHECK (
    gender IN ('MALE', 'FEMALE', 'OTHER')
  ),

  avatar TEXT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);


CREATE TABLE user_tiers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL,
  tier_id UUID NOT NULL,

  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,

  note TEXT,

  CONSTRAINT fk_user_tiers_tier
    FOREIGN KEY (tier_id) REFERENCES customer_tiers(id)
);


CREATE UNIQUE INDEX uniq_active_tier_per_user
ON user_tiers(user_id)
WHERE expires_at IS NULL;


CREATE TABLE customer_addresses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL,

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


CREATE UNIQUE INDEX uniq_default_address_per_user
ON customer_addresses(user_id)
WHERE is_default = TRUE;


CREATE TABLE loyalty_accounts (
  user_id UUID PRIMARY KEY,

  total_spent DECIMAL(14,2) DEFAULT 0 
    CHECK (total_spent >= 0),

  points_balance INT DEFAULT 0 
    CHECK (points_balance >= 0),

  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE loyalty_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL,

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

  user_id UUID NOT NULL,

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
CREATE INDEX idx_customer_addresses_user_id 
ON customer_addresses(user_id);

CREATE INDEX idx_customer_addresses_default 
ON customer_addresses(user_id, is_default);

-- tiers
CREATE INDEX idx_user_tiers_user_id 
ON user_tiers(user_id);

CREATE INDEX idx_user_tiers_tier_id 
ON user_tiers(tier_id);

-- loyalty
CREATE INDEX idx_loyalty_transactions_user_id 
ON loyalty_transactions(user_id);

CREATE INDEX idx_loyalty_transactions_type 
ON loyalty_transactions(transaction_type);

-- activity logs
CREATE INDEX idx_customer_activity_user 
ON customer_activity_logs(user_id);

CREATE INDEX idx_customer_activity_entity 
ON customer_activity_logs(entity_type, entity_id);

CREATE INDEX idx_customer_activity_created_at 
ON customer_activity_logs(created_at);
