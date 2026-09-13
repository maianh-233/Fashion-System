ALTER TABLE positions
    ADD COLUMN hierarchy_level INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN min_salary BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN max_salary BIGINT NOT NULL DEFAULT 0;

ALTER TABLE positions
    ADD CONSTRAINT ck_positions_hierarchy_level CHECK (hierarchy_level > 0),
    ADD CONSTRAINT ck_positions_min_salary CHECK (min_salary >= 0),
    ADD CONSTRAINT ck_positions_salary_range CHECK (max_salary >= min_salary);
