-- Minimal pre-V22 schema, intentionally isolated from application databases.
CREATE TABLE users(id uuid PRIMARY KEY);
CREATE TABLE stores(id uuid PRIMARY KEY);
CREATE TABLE suppliers(id uuid PRIMARY KEY);
CREATE TABLE products(id uuid PRIMARY KEY);
CREATE TABLE product_variants(id uuid PRIMARY KEY, product_id uuid NOT NULL REFERENCES products(id));
CREATE TABLE inventory_balances(store_id uuid NOT NULL REFERENCES stores(id), product_variant_id uuid NOT NULL,
 available_quantity int NOT NULL DEFAULT 0,reserved_quantity int NOT NULL DEFAULT 0,damaged_quantity int NOT NULL DEFAULT 0,
 updated_at timestamp DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(store_id,product_variant_id));
CREATE TABLE goods_receipts(id uuid PRIMARY KEY,receipt_code varchar(50),supplier_id uuid REFERENCES suppliers(id),
 store_id uuid NOT NULL REFERENCES stores(id),received_by uuid,approved_by uuid,status varchar(50) NOT NULL,updated_at timestamp);
CREATE TABLE goods_issues(id uuid PRIMARY KEY,issue_code varchar(50),store_id uuid NOT NULL REFERENCES stores(id),
 issued_by uuid,approved_by uuid,issue_type varchar(50) NOT NULL,status varchar(50) NOT NULL,updated_at timestamp);
CREATE TABLE goods_receipt_items(id uuid PRIMARY KEY,receipt_id uuid NOT NULL REFERENCES goods_receipts(id),product_variant_id uuid NOT NULL,quantity int NOT NULL,cost_price numeric(12,2) NOT NULL DEFAULT 0);
CREATE TABLE goods_issue_items(id uuid PRIMARY KEY,issue_id uuid NOT NULL REFERENCES goods_issues(id),product_variant_id uuid NOT NULL,quantity int NOT NULL);
CREATE TABLE inventory_transactions(id uuid PRIMARY KEY,product_variant_id uuid NOT NULL,store_id uuid NOT NULL REFERENCES stores(id),
 transaction_type varchar(50) NOT NULL,reference_type varchar(50),reference_id uuid,quantity int NOT NULL,balance_after int NOT NULL,created_by uuid,created_at timestamp);
CREATE TABLE permission_groups(id uuid PRIMARY KEY,code varchar(50) UNIQUE NOT NULL,name varchar(100) NOT NULL);
CREATE TABLE permissions(id uuid PRIMARY KEY DEFAULT gen_random_uuid(),code varchar(100) UNIQUE NOT NULL,name varchar(150) NOT NULL,
 group_id uuid NOT NULL REFERENCES permission_groups(id),description text);
CREATE TABLE roles(id uuid PRIMARY KEY,code varchar(50) UNIQUE NOT NULL);
CREATE TABLE role_permissions(role_id uuid REFERENCES roles(id),permission_id uuid REFERENCES permissions(id),scope varchar(20) NOT NULL,PRIMARY KEY(role_id,permission_id));
CREATE TABLE user_permissions(user_id uuid REFERENCES users(id),permission_id uuid REFERENCES permissions(id),effect varchar(10) NOT NULL,scope varchar(20) NOT NULL,PRIMARY KEY(user_id,permission_id));
INSERT INTO users VALUES ('00000000-0000-0000-0000-000000000001');
INSERT INTO stores VALUES ('00000000-0000-0000-0000-000000000002');
INSERT INTO suppliers VALUES ('00000000-0000-0000-0000-000000000016');
INSERT INTO products VALUES ('00000000-0000-0000-0000-000000000003'),('00000000-0000-0000-0000-000000000009');
INSERT INTO product_variants VALUES ('00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000003');
INSERT INTO inventory_balances(store_id,product_variant_id,available_quantity,reserved_quantity,damaged_quantity)
VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000004',10,2,1);
INSERT INTO goods_receipts VALUES ('00000000-0000-0000-0000-000000000005','OLD-IN',NULL,'00000000-0000-0000-0000-000000000002',
 '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','APPROVED','2026-01-02');
INSERT INTO goods_receipts VALUES ('00000000-0000-0000-0000-000000000010','DRAFT-IN',NULL,'00000000-0000-0000-0000-000000000002',NULL,NULL,'PENDING',NULL);
INSERT INTO goods_issues VALUES ('00000000-0000-0000-0000-000000000006','OLD-OUT','00000000-0000-0000-0000-000000000002',
 '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','SALE','APPROVED','2026-01-03');
INSERT INTO goods_receipt_items VALUES ('00000000-0000-0000-0000-000000000007','00000000-0000-0000-0000-000000000005','00000000-0000-0000-0000-000000000004',20,1.50);
INSERT INTO goods_issue_items VALUES ('00000000-0000-0000-0000-000000000008','00000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000004',10);
INSERT INTO inventory_transactions VALUES ('00000000-0000-0000-0000-000000000011','00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000002',
 'ISSUE','ORDER','00000000-0000-0000-0000-000000000099',10,10,'00000000-0000-0000-0000-000000000001','2026-01-03');
INSERT INTO permission_groups VALUES ('00000000-0000-0000-0000-000000000012','IMPORT_RECEIPT','Import'),('00000000-0000-0000-0000-000000000013','EXPORT_RECEIPT','Export');
INSERT INTO permissions(code,name,group_id) SELECT g.code || '_' || a,'Legacy ' || a,g.id FROM permission_groups g CROSS JOIN unnest(ARRAY['APPROVE','DELETE']) a;
INSERT INTO roles VALUES ('00000000-0000-0000-0000-000000000014','WAREHOUSE'),('00000000-0000-0000-0000-000000000015','ADMIN');
INSERT INTO role_permissions SELECT '00000000-0000-0000-0000-000000000014',id,'STORE' FROM permissions;
INSERT INTO user_permissions SELECT '00000000-0000-0000-0000-000000000001',id,'DENY','STORE' FROM permissions WHERE code LIKE '%APPROVE';
