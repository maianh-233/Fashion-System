"""Verify Customer-domain migrations against an isolated PostgreSQL schema."""

import argparse
from pathlib import Path
import subprocess
import uuid


parser = argparse.ArgumentParser()
parser.add_argument("--psql", default="psql")
parser.add_argument("--host", default="127.0.0.1")
parser.add_argument("--port", default="5432")
parser.add_argument("--user", default="postgres")
parser.add_argument("--database", default="commerce_db")
args = parser.parse_args()

root = Path(__file__).resolve().parents[1]
v24_path = root / "src/main/resources/db/migration/V24__customer_auth_security.sql"
v25_path = root / "src/main/resources/db/migration/V25__customer_domain_and_auth_isolation.sql"
v26_path = root / "src/main/resources/db/migration/V26__customer_management_permissions.sql"
if not v25_path.exists():
    raise AssertionError(f"Missing migration: {v25_path}")

v24 = v24_path.read_text(encoding="utf-8")
v25 = v25_path.read_text(encoding="utf-8")
v26 = v26_path.read_text(encoding="utf-8")
schema = "customer_migration_verify_" + uuid.uuid4().hex
command = [
    args.psql, "-X", "-q", "-t", "-A", "-v", "ON_ERROR_STOP=1",
    "-h", args.host, "-p", args.port, "-U", args.user, "-d", args.database,
]


def sql(statement: str) -> str:
    result = subprocess.run(
        command,
        input=f"SET search_path TO {schema},public;\n{statement}",
        text=True,
        encoding="utf-8",
        capture_output=True,
        timeout=60,
    )
    if result.returncode:
        raise AssertionError(result.stderr)
    return result.stdout.strip()


fixture = """
CREATE TABLE modules(
    id uuid PRIMARY KEY, code varchar(50) UNIQUE NOT NULL, name varchar(100) NOT NULL,
    description text, icon varchar(100), sort_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true, created_at timestamp NOT NULL
);
CREATE TABLE permission_groups(
    id uuid PRIMARY KEY, module_id uuid NOT NULL REFERENCES modules(id),
    name varchar(100) NOT NULL, code varchar(50) UNIQUE NOT NULL,
    description text, created_at timestamp
);
CREATE TABLE permissions(
    id uuid PRIMARY KEY, name varchar(150) NOT NULL, code varchar(100) UNIQUE NOT NULL,
    group_id uuid NOT NULL REFERENCES permission_groups(id), description text, created_at timestamp
);
CREATE TABLE roles(id uuid PRIMARY KEY, code varchar(50) UNIQUE NOT NULL);
CREATE TABLE role_permissions(
    role_id uuid NOT NULL REFERENCES roles(id), permission_id uuid NOT NULL REFERENCES permissions(id),
    scope varchar(20) NOT NULL, PRIMARY KEY(role_id,permission_id)
);
CREATE TABLE users(
    id uuid PRIMARY KEY,
    username varchar(50) UNIQUE NOT NULL,
    password_hash text NOT NULL
);
CREATE TABLE stores(
    id uuid PRIMARY KEY,
    code varchar(50) UNIQUE NOT NULL,
    name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true
);
CREATE TABLE customers(
    id uuid PRIMARY KEY,
    username varchar(50) UNIQUE NOT NULL,
    email varchar(255) UNIQUE,
    phone varchar(20) UNIQUE,
    password_hash text,
    active boolean,
    locked boolean,
    full_name varchar(255),
    date_of_birth date,
    gender varchar(20),
    avatar text,
    created_at timestamp,
    updated_at timestamp
);
CREATE TABLE customer_addresses(
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    customer_id uuid NOT NULL,
    receiver_name varchar(255) NOT NULL,
    receiver_phone varchar(20) NOT NULL,
    address_line text NOT NULL,
    is_default boolean,
    created_at timestamp,
    updated_at timestamp
);
CREATE UNIQUE INDEX uq_customer_addresses_default_user
    ON customer_addresses(user_id) WHERE is_default = true;
CREATE TABLE customer_social_accounts(
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL,
    provider varchar(20) NOT NULL,
    provider_user_id varchar(255) NOT NULL,
    provider_email varchar(255),
    created_at timestamp,
    last_login_at timestamp,
    UNIQUE(provider, provider_user_id),
    UNIQUE(customer_id, provider)
);
CREATE TABLE customer_tiers(
    id uuid PRIMARY KEY,
    code varchar(50) UNIQUE NOT NULL,
    name varchar(100) NOT NULL,
    min_total_spent numeric(14,2),
    discount_percent numeric(5,2),
    created_at timestamp,
    updated_at timestamp
);
CREATE TABLE customer_tier_assignments(
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL,
    tier_id uuid NOT NULL,
    assigned_at timestamp,
    expires_at timestamp,
    note text
);
CREATE TABLE user_tokens(
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    customer_id uuid,
    token_hash text NOT NULL,
    token_type varchar(30),
    refresh_token_family uuid,
    parent_token_id uuid,
    expires_at timestamp NOT NULL,
    revoked_at timestamp,
    device varchar(100),
    ip_address varchar(50),
    user_agent text,
    created_at timestamp
);
CREATE TABLE password_reset_otps(
    id uuid PRIMARY KEY,
    account_type varchar(20) NOT NULL,
    account_id uuid NOT NULL,
    email varchar(255) NOT NULL,
    otp_hash text NOT NULL,
    reset_token_hash varchar(64),
    expires_at timestamp NOT NULL,
    reset_token_expires_at timestamp,
    verified_at timestamp,
    used_at timestamp,
    failed_attempts integer NOT NULL,
    created_at timestamp NOT NULL
);

INSERT INTO users VALUES
 ('10000000-0000-0000-0000-000000000001', 'employee.one', 'employee-hash');
INSERT INTO modules VALUES
 ('70000000-0000-0000-0000-000000000001','CUSTOMER','Customer',NULL,NULL,1,true,now());
INSERT INTO roles VALUES
 ('71000000-0000-0000-0000-000000000001','ADMIN'),
 ('71000000-0000-0000-0000-000000000002','STAFF');
INSERT INTO stores VALUES
 ('20000000-0000-0000-0000-000000000001', 'HCM01', 'HCM 01', true);
INSERT INTO customers(id,username,email,phone,password_hash,active,locked,full_name,created_at,updated_at) VALUES
 ('30000000-0000-0000-0000-000000000001','web.one','web.one@example.com','0901234567','customer-hash',true,false,'Web One',now(),now()),
 ('30000000-0000-0000-0000-000000000002','social.one','social.one@example.com','+84987654321',NULL,true,false,'Social One',now(),now());
INSERT INTO customer_social_accounts VALUES
 ('40000000-0000-0000-0000-000000000001','30000000-0000-0000-0000-000000000002','GOOGLE','google-subject','social.one@example.com',now(),now());
INSERT INTO user_tokens(id,user_id,token_hash,token_type,refresh_token_family,expires_at,created_at) VALUES
 ('50000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','employee-token','REFRESH','50000000-0000-0000-0000-000000000002',now() + interval '1 day',now());
INSERT INTO password_reset_otps VALUES
 ('60000000-0000-0000-0000-000000000001','EMPLOYEE','10000000-0000-0000-0000-000000000001','employee@example.com','otp',NULL,now()+interval '5 minutes',NULL,NULL,NULL,0,now()),
 ('60000000-0000-0000-0000-000000000002','CUSTOMER','30000000-0000-0000-0000-000000000001','web.one@example.com','otp',NULL,now()+interval '5 minutes',NULL,NULL,NULL,0,now());
"""


sql(f"CREATE SCHEMA {schema};")
try:
    sql(fixture)
    users_before = sql("SELECT string_agg(id::text || ':' || username || ':' || password_hash, ',' ORDER BY id) FROM users;")
    tokens_before = sql("SELECT string_agg(id::text || ':' || user_id::text || ':' || token_hash, ',' ORDER BY id) FROM user_tokens;")

    sql(v24)
    sql(v25)
    sql(v26)

    assert sql("SELECT count(*) FROM customer_accounts;") == "2"
    assert sql("SELECT count(*) FROM customer_accounts WHERE customer_id IS NOT NULL;") == "2"
    assert sql("SELECT count(*) FROM customers WHERE source='WEBSITE' AND origin_store_id IS NULL;") == "2"
    assert sql("SELECT count(*) FROM customers WHERE customer_code ~ '^CUS[0-9]{6,}$';") == "2"
    assert sql("SELECT normalized_phone FROM customers WHERE id='30000000-0000-0000-0000-000000000001';") == "+84901234567"
    assert sql("SELECT normalized_phone FROM customers WHERE id='30000000-0000-0000-0000-000000000002';") == "+84987654321"
    assert sql("SELECT count(*) FROM customer_tiers WHERE code='REGULAR';") == "1"
    assert sql("SELECT count(*) FROM customer_tier_assignments WHERE expires_at IS NULL;") == "2"
    assert sql("SELECT count(*) FROM customer_social_accounts s JOIN customers c ON c.id=s.customer_id;") == "1"
    assert sql("SELECT count(*) FROM password_reset_otps WHERE account_type='CUSTOMER';") == "0"
    assert sql("SELECT count(*) FROM customer_password_reset_challenges;") == "1"
    assert sql("SELECT count(*) FROM user_tokens;") == "1"
    assert sql("SELECT string_agg(id::text || ':' || username || ':' || password_hash, ',' ORDER BY id) FROM users;") == users_before
    assert sql("SELECT string_agg(id::text || ':' || user_id::text || ':' || token_hash, ',' ORDER BY id) FROM user_tokens;") == tokens_before
    assert sql("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='customers' AND column_name IN ('username','password_hash','locked','failed_login_attempts','login_locked_until');") == "0"
    assert sql("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='user_tokens' AND column_name='customer_id';") == "0"
    assert sql("SELECT count(*) FROM permissions WHERE code LIKE 'CUSTOMER_%';") == "5"
    assert sql("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id JOIN permissions p ON p.id=rp.permission_id WHERE r.code='ADMIN' AND rp.scope='ALL' AND p.code LIKE 'CUSTOMER_%';") == "5"
    assert sql("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id=rp.role_id JOIN permissions p ON p.id=rp.permission_id WHERE r.code='STAFF' AND rp.scope='STORE' AND p.code IN ('CUSTOMER_VIEW','CUSTOMER_CREATE','CUSTOMER_UPDATE','CUSTOMER_STATUS_MANAGE');") == "4"
    assert sql("SELECT is_nullable FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='customer_addresses' AND column_name='user_id';") in {"YES", ""}
    print("PASS Customer migration isolates auth persistence and preserves Employee/Admin account data")
finally:
    sql(f"DROP SCHEMA {schema} CASCADE;")
