-- Danh mục vị trí phục vụ nhân sự hiện tại và module tuyển dụng sau này.
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
  ADD COLUMN position_id UUID,
  ADD CONSTRAINT fk_users_position
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE SET NULL;

CREATE INDEX idx_users_position_id ON users(position_id);
