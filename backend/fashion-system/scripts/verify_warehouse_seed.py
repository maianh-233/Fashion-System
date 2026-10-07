"""Verify the development warehouse seed against an isolated PostgreSQL schema."""

import argparse
from pathlib import Path
import subprocess
import uuid


parser = argparse.ArgumentParser()
parser.add_argument("--psql", default="psql")
parser.add_argument("--host", default="127.0.0.1")
parser.add_argument("--port", default="55432")
parser.add_argument("--user", default="warehouse_test")
parser.add_argument("--database", default="warehouse_seed_test")
args = parser.parse_args()
if not args.database.startswith("warehouse_"):
    parser.error("Use a disposable database whose name starts with warehouse_")

root = Path(__file__).resolve().parents[1]
seed_path = root / "src/main/resources/db/seed/warehouse_demo_data.sql"
seed = seed_path.read_text(encoding="utf-8")
schema = "warehouse_seed_verify_" + uuid.uuid4().hex
command = [
    args.psql,
    "-X",
    "-q",
    "-t",
    "-A",
    "-v",
    "ON_ERROR_STOP=1",
    "-h",
    args.host,
    "-p",
    args.port,
    "-U",
    args.user,
    "-d",
    args.database,
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
CREATE TABLE users(id uuid PRIMARY KEY);
CREATE TABLE stores(id uuid PRIMARY KEY, code varchar(50) UNIQUE NOT NULL, active boolean);
CREATE TABLE suppliers(
    id uuid PRIMARY KEY, code varchar(50) UNIQUE, name varchar(255) NOT NULL,
    contact_name varchar(255), phone varchar(20) UNIQUE, email varchar(255) UNIQUE,
    address text, status varchar(50), created_at timestamp, updated_at timestamp
);
CREATE TABLE products(id uuid PRIMARY KEY, name varchar(255) NOT NULL);
CREATE TABLE product_variants(
    id uuid PRIMARY KEY, product_id uuid NOT NULL REFERENCES products(id),
    sku varchar(100) UNIQUE NOT NULL, active boolean
);
CREATE UNIQUE INDEX uq_warehouse_variant_product ON product_variants(id, product_id);
CREATE TABLE goods_receipts(
    id uuid PRIMARY KEY, receipt_code varchar(50) UNIQUE NOT NULL,
    supplier_id uuid REFERENCES suppliers(id), store_id uuid NOT NULL REFERENCES stores(id),
    received_by uuid REFERENCES users(id), approved_by uuid REFERENCES users(id),
    receipt_date timestamp, status varchar(50) NOT NULL, note text,
    total_quantity integer, total_amount numeric(14,2), created_at timestamp,
    updated_at timestamp, confirmed_at timestamp, completed_at timestamp,
    completed_by uuid REFERENCES users(id),
    CONSTRAINT ck_warehouse_receipt_status CHECK (status IN
      ('DRAFT','PENDING_CONFIRMATION','CONFIRMED','COMPLETED','CANCELLED'))
);
CREATE TABLE goods_receipt_items(
    id uuid PRIMARY KEY, receipt_id uuid NOT NULL REFERENCES goods_receipts(id),
    product_variant_id uuid NOT NULL, sku varchar(100), product_name varchar(255),
    cost_price numeric(12,2) NOT NULL CHECK (cost_price >= 0),
    quantity integer NOT NULL CHECK (quantity > 0), total numeric(14,2) NOT NULL,
    created_at timestamp, product_id uuid NOT NULL REFERENCES products(id),
    target_channel varchar(20) NOT NULL CHECK (target_channel IN ('ONLINE','OFFLINE')),
    FOREIGN KEY (product_variant_id, product_id) REFERENCES product_variants(id, product_id)
);
CREATE TABLE goods_issues(
    id uuid PRIMARY KEY, issue_code varchar(50) UNIQUE NOT NULL,
    store_id uuid NOT NULL REFERENCES stores(id), order_id uuid,
    issued_by uuid REFERENCES users(id), approved_by uuid REFERENCES users(id),
    issue_type varchar(50) NOT NULL CHECK (issue_type IN
      ('RETURN_TO_SUPPLIER','DAMAGED','OTHER','ONLINE_TO_OFFLINE','OFFLINE_TO_ONLINE')),
    issue_date timestamp, status varchar(50) NOT NULL CHECK (status IN
      ('DRAFT','PENDING_CONFIRMATION','CONFIRMED','COMPLETED','CANCELLED')),
    note text, total_quantity integer, created_at timestamp, updated_at timestamp,
    confirmed_at timestamp, completed_at timestamp, completed_by uuid REFERENCES users(id),
    supplier_id uuid REFERENCES suppliers(id), reason text
);
CREATE TABLE goods_issue_items(
    id uuid PRIMARY KEY, issue_id uuid NOT NULL REFERENCES goods_issues(id),
    product_variant_id uuid NOT NULL, sku varchar(100), product_name varchar(255),
    quantity integer NOT NULL CHECK (quantity > 0), created_at timestamp,
    product_id uuid NOT NULL REFERENCES products(id),
    source_channel varchar(20) NOT NULL CHECK (source_channel IN ('ONLINE','OFFLINE')),
    FOREIGN KEY (product_variant_id, product_id) REFERENCES product_variants(id, product_id)
);
CREATE TABLE inventory_balances(
    store_id uuid NOT NULL REFERENCES stores(id), product_variant_id uuid NOT NULL REFERENCES product_variants(id),
    available_quantity integer NOT NULL DEFAULT 0, online_quantity integer NOT NULL DEFAULT 0,
    reserved_quantity integer NOT NULL DEFAULT 0, damaged_quantity integer NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0, updated_at timestamp,
    PRIMARY KEY(store_id, product_variant_id),
    CHECK (available_quantity >= 0 AND online_quantity >= 0 AND reserved_quantity >= 0 AND damaged_quantity >= 0)
);
CREATE TABLE inventory_transactions(
    id uuid PRIMARY KEY, product_variant_id uuid NOT NULL REFERENCES product_variants(id),
    store_id uuid NOT NULL REFERENCES stores(id), transaction_type varchar(50) NOT NULL,
    reference_type varchar(50), reference_id uuid, quantity integer NOT NULL,
    balance_after integer NOT NULL, before_offline integer, after_offline integer,
    before_online integer, after_online integer, from_channel varchar(20), to_channel varchar(20),
    import_receipt_id uuid REFERENCES goods_receipts(id), export_receipt_id uuid REFERENCES goods_issues(id),
    created_by uuid REFERENCES users(id), created_at timestamp,
    CHECK (from_channel IS NULL OR from_channel IN ('ONLINE','OFFLINE')),
    CHECK (to_channel IS NULL OR to_channel IN ('ONLINE','OFFLINE'))
);

INSERT INTO users VALUES ('10000000-0000-0000-0000-000000000001');
INSERT INTO stores VALUES ('20000000-0000-0000-0000-000000000001', 'HCM-DEMO', true);
INSERT INTO products VALUES
 ('30000000-0000-0000-0000-000000000001', 'Ao so mi demo'),
 ('30000000-0000-0000-0000-000000000002', 'Quan tay demo');
INSERT INTO product_variants VALUES
 ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'DEMO-SM-M', true),
 ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', 'DEMO-QT-L', true);
"""

sql(f"CREATE SCHEMA {schema};")
try:
    sql(fixture)
    sql(seed)
    sql(seed)

    assert sql("SELECT count(*) FROM suppliers WHERE code LIKE 'NCC-DEMO-%';") == "5"
    assert sql("SELECT count(*) FROM goods_receipts WHERE receipt_code LIKE 'DEMO-PN-%';") == "5"
    assert sql("SELECT count(*) FROM goods_issues WHERE issue_code LIKE 'DEMO-PX-%';") == "7"
    assert sql("SELECT count(*) FROM inventory_balances;") == "2"
    assert sql("SELECT count(*) FROM inventory_transactions;") == "10"
    assert sql("SELECT count(*) FROM inventory_balances WHERE available_quantity=40 AND online_quantity=15 AND damaged_quantity=2;") == "2"
    assert sql("SELECT count(DISTINCT status) FROM goods_receipts WHERE receipt_code LIKE 'DEMO-PN-%';") == "5"
    assert sql("SELECT count(DISTINCT status) FROM goods_issues WHERE issue_code LIKE 'DEMO-PX-%';") == "5"
    assert sql("SELECT count(DISTINCT issue_type) FROM goods_issues WHERE issue_code LIKE 'DEMO-PX-%';") == "5"
    assert sql("SELECT count(*) FROM goods_receipt_items i JOIN product_variants v ON (v.id,v.product_id)=(i.product_variant_id,i.product_id) WHERE i.receipt_id='da000000-0000-0000-0000-000000000001';") == "4"
    assert sql("SELECT count(*) FROM goods_issue_items i JOIN product_variants v ON (v.id,v.product_id)=(i.product_variant_id,i.product_id) WHERE i.issue_id IN ('db000000-0000-0000-0000-000000000001','db000000-0000-0000-0000-000000000002','db000000-0000-0000-0000-000000000003');") == "6"
    print("PASS warehouse seed is repeatable and produces consistent suppliers, workflows, balances, and ledger rows")
finally:
    sql(f"DROP SCHEMA {schema} CASCADE;")
