BEGIN;

ALTER TABLE users
  ADD COLUMN employee_code VARCHAR(30),
  ADD COLUMN full_name VARCHAR(255),
  ADD COLUMN date_of_birth DATE,
  ADD COLUMN gender VARCHAR(20),
  ADD COLUMN avatar TEXT,
  ADD COLUMN job_title VARCHAR(150),
  ADD COLUMN employment_type VARCHAR(30),
  ADD COLUMN employment_status VARCHAR(30) DEFAULT 'ACTIVE',
  ADD COLUMN hire_date DATE,
  ADD COLUMN termination_date DATE,
  ADD COLUMN work_location VARCHAR(150),
  ADD COLUMN manager_id UUID;

ALTER TABLE users
  ADD CONSTRAINT uq_users_employee_code UNIQUE (employee_code),
  ADD CONSTRAINT chk_users_gender
    CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
  ADD CONSTRAINT chk_users_employment_type
    CHECK (employment_type IS NULL OR employment_type IN (
      'FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN', 'TEMPORARY'
    )),
  ADD CONSTRAINT chk_users_employment_status
    CHECK (employment_status IN ('ACTIVE', 'PROBATION', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED')),
  ADD CONSTRAINT chk_users_employment_dates
    CHECK (termination_date IS NULL OR hire_date IS NULL OR termination_date >= hire_date),
  ADD CONSTRAINT fk_users_manager
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_employment_status ON users(employment_status);

COMMIT;
