DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM inventory_balances WHERE available_quantity=10 AND online_quantity=0 AND reserved_quantity=2 AND damaged_quantity=1 AND version=0) THEN RAISE EXCEPTION 'Stock changed'; END IF;
 IF NOT EXISTS (SELECT 1 FROM goods_receipts WHERE receipt_code='OLD-IN' AND status='COMPLETED' AND supplier_id IS NULL AND completed_by=approved_by AND completed_at='2026-01-02') THEN RAISE EXCEPTION 'Historical import lost'; END IF;
 IF NOT EXISTS (SELECT 1 FROM goods_receipts WHERE receipt_code='DRAFT-IN' AND status='DRAFT') THEN RAISE EXCEPTION 'Pending conversion failed'; END IF;
 IF NOT EXISTS (SELECT 1 FROM goods_issues WHERE status='COMPLETED' AND issue_type='OTHER' AND reason='Legacy issue type: SALE') THEN RAISE EXCEPTION 'Historical export lost'; END IF;
 IF NOT EXISTS (SELECT 1 FROM goods_receipt_items WHERE product_id='00000000-0000-0000-0000-000000000003' AND target_channel='OFFLINE') THEN RAISE EXCEPTION 'Variant backfill failed'; END IF;
 IF NOT EXISTS (SELECT 1 FROM inventory_transactions WHERE reference_id='00000000-0000-0000-0000-000000000099' AND import_receipt_id IS NULL AND export_receipt_id IS NULL AND before_offline IS NULL) THEN RAISE EXCEPTION 'Ledger history changed'; END IF;
 IF (SELECT count(*) FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id JOIN roles r ON r.id=rp.role_id WHERE r.code='WAREHOUSE' AND (p.code LIKE '%CONFIRM' OR p.code LIKE '%CANCEL') AND rp.scope='STORE') <> 4 THEN RAISE EXCEPTION 'Scoped grants lost'; END IF;
 IF (SELECT count(*) FROM user_permissions up JOIN permissions p ON p.id=up.permission_id WHERE p.code LIKE '%CONFIRM' AND effect='DENY' AND scope='STORE') <> 2 THEN RAISE EXCEPTION 'Deny overrides lost'; END IF;
 IF EXISTS (SELECT 1 FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id JOIN roles r ON r.id=rp.role_id WHERE r.code='WAREHOUSE' AND p.code LIKE '%COMPLETE') THEN RAISE EXCEPTION 'COMPLETE overgranted'; END IF;
 BEGIN UPDATE inventory_balances SET available_quantity=-1; RAISE EXCEPTION 'Negative stock accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE inventory_balances SET available_quantity=2147483647,online_quantity=1; RAISE EXCEPTION 'Overflow accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_receipt_items SET product_id='00000000-0000-0000-0000-000000000009'; RAISE EXCEPTION 'Mismatched variant accepted'; EXCEPTION WHEN foreign_key_violation THEN NULL; END;
 BEGIN UPDATE goods_issues SET status='UNKNOWN'; RAISE EXCEPTION 'Unknown state accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_issue_items SET source_channel='WAREHOUSE'; RAISE EXCEPTION 'Unknown channel accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE inventory_transactions SET export_receipt_id='00000000-0000-0000-0000-000000000099'; RAISE EXCEPTION 'Orphan reference accepted'; EXCEPTION WHEN foreign_key_violation THEN NULL; END;
 BEGIN UPDATE goods_receipt_items SET quantity=0; RAISE EXCEPTION 'Zero import quantity accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_issue_items SET quantity=-1; RAISE EXCEPTION 'Negative export quantity accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_receipt_items SET cost_price=-0.01; RAISE EXCEPTION 'Negative import cost accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_receipts SET status='PENDING_CONFIRMATION' WHERE receipt_code='DRAFT-IN'; RAISE EXCEPTION 'Submitted import without supplier accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_issues SET reason='  '; RAISE EXCEPTION 'OTHER without reason accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN UPDATE goods_issues SET issue_type='RETURN_TO_SUPPLIER'; RAISE EXCEPTION 'Supplier return without supplier accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
END $$;
-- Every state accepted by the service must be representable in the database.
DO $$ DECLARE state text; receipt uuid; issue uuid; BEGIN
 FOREACH state IN ARRAY ARRAY['DRAFT','PENDING_CONFIRMATION','CONFIRMED','COMPLETED','CANCELLED'] LOOP
   receipt := gen_random_uuid(); issue := gen_random_uuid();
   INSERT INTO goods_receipts(id,receipt_code,store_id,supplier_id,status)
   VALUES(receipt,'STATE-' || receipt,'00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000016',state);
   INSERT INTO goods_issues(id,issue_code,store_id,issue_type,reason,status)
   VALUES(issue,'STATE-' || issue,'00000000-0000-0000-0000-000000000002','OTHER','State compatibility check',state);
   DELETE FROM goods_receipts WHERE id=receipt;
   DELETE FROM goods_issues WHERE id=issue;
 END LOOP;
 -- Submit then confirm both receipt types without changing inventory.
 UPDATE goods_receipts SET supplier_id='00000000-0000-0000-0000-000000000016',status='PENDING_CONFIRMATION' WHERE receipt_code='DRAFT-IN';
 UPDATE goods_receipts SET status='CONFIRMED' WHERE receipt_code='DRAFT-IN';
 receipt := gen_random_uuid();
 INSERT INTO goods_issues(id,issue_code,store_id,issue_type,reason,status)
 VALUES(receipt,'SUBMIT-TEST','00000000-0000-0000-0000-000000000002','OTHER','Submit test','DRAFT');
 UPDATE goods_issues SET status='PENDING_CONFIRMATION' WHERE id=receipt;
 UPDATE goods_issues SET status='CONFIRMED' WHERE id=receipt;
 IF NOT EXISTS (SELECT 1 FROM goods_receipts WHERE receipt_code='DRAFT-IN' AND status='CONFIRMED')
 OR NOT EXISTS (SELECT 1 FROM goods_issues WHERE id=receipt AND status='CONFIRMED') THEN RAISE EXCEPTION 'Submit/confirm state persistence failed'; END IF;
 IF (SELECT available_quantity FROM inventory_balances LIMIT 1) <> 10 THEN RAISE EXCEPTION 'Submit/confirm changed stock'; END IF;
 DELETE FROM goods_issues WHERE id=receipt;
 UPDATE goods_receipts SET status='DRAFT',supplier_id=NULL WHERE receipt_code='DRAFT-IN';
END $$;
SELECT 'warehouse migration assertions passed' AS result;
