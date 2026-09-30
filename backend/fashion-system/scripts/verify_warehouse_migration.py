"""Run against a disposable warehouse_* database; requires psql, no Python packages."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import subprocess
import uuid

parser = argparse.ArgumentParser()
parser.add_argument('--psql', default='psql')
parser.add_argument('--host', default='127.0.0.1')
parser.add_argument('--port', default='55432')
parser.add_argument('--user', default='warehouse_test')
parser.add_argument('--database', default='warehouse_migration_test')
args = parser.parse_args()
if not args.database.startswith('warehouse_'):
    parser.error('Use a disposable database whose name starts with warehouse_')
root = Path(__file__).resolve().parents[1]
fixture = (root / 'src/test/resources/warehouse-migration-fixture.sql').read_text(encoding='utf-8')
migration = (root / 'src/main/resources/db/migration/V22__store_warehouse_workflow.sql').read_text(encoding='utf-8')
assertions = (root / 'src/test/resources/warehouse-migration-assertions.sql').read_text(encoding='utf-8')
command = [args.psql, '-X', '-q', '-t', '-A', '-v', 'ON_ERROR_STOP=1', '-h', args.host,
           '-p', args.port, '-U', args.user, '-d', args.database]
schema = 'warehouse_verify_' + uuid.uuid4().hex


def sql(statement, *, expect_success=True):
    result = subprocess.run(command, input=f'SET search_path TO {schema},public;\n' + statement,
                            text=True, encoding='utf-8', capture_output=True, timeout=60)
    if expect_success and result.returncode:
        raise AssertionError(result.stderr)
    return result


sql(f'CREATE SCHEMA {schema};')
try:
    sql(fixture)
    # Unknown states fail before changes. Every failed invocation disconnects,
    # causing PostgreSQL to roll back its open transaction.
    sql("UPDATE goods_receipts SET status='MYSTERY' WHERE receipt_code='OLD-IN';")
    assert sql(migration, expect_success=False).returncode != 0
    assert sql("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND column_name='online_quantity';").stdout.strip() == '0'
    sql("UPDATE goods_receipts SET status='APPROVED' WHERE receipt_code='OLD-IN';")
    # Test a late failure, after schema and status changes: orphan variant.
    sql("UPDATE goods_receipt_items SET product_variant_id='00000000-0000-0000-0000-000000000099';")
    assert sql(migration, expect_success=False).returncode != 0
    assert sql("SELECT status FROM goods_receipts WHERE receipt_code='OLD-IN';").stdout.strip() == 'APPROVED'
    assert sql("SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND column_name='online_quantity';").stdout.strip() == '0'
    sql("UPDATE goods_receipt_items SET product_variant_id='00000000-0000-0000-0000-000000000004';")
    sql(migration)
    sql(assertions)
    # Rerun while receipts are submitted: preflight must accept the full lifecycle.
    sql("UPDATE goods_receipts SET supplier_id='00000000-0000-0000-0000-000000000016',status='PENDING_CONFIRMATION' WHERE receipt_code='DRAFT-IN';")
    sql(migration)
    sql("UPDATE goods_receipts SET status='DRAFT',supplier_id=NULL WHERE receipt_code='DRAFT-IN';")
    sql(assertions)
    print('PASS migration, submitted-state rerun, all states, submit/confirm, history, grants, FK/check enforcement, early/late failure rollback')

    # The same upsert + row-lock pattern used by InventoryService: each worker
    # locks the single balance before reading its current quantity.
    key = "store_id='00000000-0000-0000-0000-000000000002' AND product_variant_id='00000000-0000-0000-0000-000000000004'"

    def export(quantity):
        return sql(f"""BEGIN;
            SELECT available_quantity FROM inventory_balances WHERE {key} FOR UPDATE;
            SELECT pg_sleep(0.15);
            DO $$ DECLARE current_stock integer; BEGIN
              SELECT available_quantity INTO current_stock FROM inventory_balances WHERE {key};
              IF current_stock < {quantity} THEN RAISE EXCEPTION 'insufficient stock'; END IF;
              UPDATE inventory_balances SET available_quantity=available_quantity-{quantity},version=version+1 WHERE {key};
            END $$;
            COMMIT;""", expect_success=False)

    with ThreadPoolExecutor(max_workers=2) as pool:
        outcomes = list(pool.map(export, [8, 5]))
    assert sorted(r.returncode == 0 for r in outcomes) == [False, True]
    assert 'insufficient stock' in next(r.stderr for r in outcomes if r.returncode)
    remaining = int(sql(f'SELECT available_quantity FROM inventory_balances WHERE {key};').stdout.strip())
    assert remaining in (2, 5)
    print(f'PASS competing exports 8 and 5 from 10: one commits, one rejects, remaining={remaining}')

    sql("INSERT INTO product_variants VALUES ('00000000-0000-0000-0000-000000000020','00000000-0000-0000-0000-000000000003');")

    def first_insert(_):
        sql("""BEGIN;
          INSERT INTO inventory_balances(store_id,product_variant_id)
          VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000020') ON CONFLICT DO NOTHING;
          SELECT available_quantity FROM inventory_balances WHERE product_variant_id='00000000-0000-0000-0000-000000000020' FOR UPDATE;
          SELECT pg_sleep(0.15);
          UPDATE inventory_balances SET available_quantity=available_quantity+1,version=version+1 WHERE product_variant_id='00000000-0000-0000-0000-000000000020';
          COMMIT;""")

    with ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(first_insert, range(2)))
    assert sql("SELECT count(*) || ':' || sum(available_quantity) FROM inventory_balances WHERE product_variant_id='00000000-0000-0000-0000-000000000020';").stdout.strip() == '1:2'
    print('PASS simultaneous first insert: one composite balance row, both increments preserved')
finally:
    sql(f'DROP SCHEMA {schema} CASCADE;')
