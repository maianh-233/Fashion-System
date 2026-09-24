CREATE SEQUENCE IF NOT EXISTS supplier_code_seq;

SELECT setval('supplier_code_seq', GREATEST(1,
    COALESCE((SELECT MAX(substring(code FROM 4)::bigint)
              FROM suppliers WHERE code ~ '^NCC[0-9]+$'), 0) + 1), false);

UPDATE suppliers SET email = NULLIF(lower(trim(email)), '') WHERE email IS NOT NULL;
UPDATE suppliers SET phone = NULLIF(regexp_replace(trim(phone), '[[:space:]()-]', '', 'g'), '')
WHERE phone IS NOT NULL;

-- Preserve the oldest supplier's normalized contact value. Older schemas did not
-- consistently enforce these constraints, so later duplicates are cleared instead
-- of blocking this migration on an existing database.
WITH duplicate_emails AS (
    SELECT id, row_number() OVER (
        PARTITION BY email ORDER BY created_at NULLS LAST, id
    ) AS duplicate_number
    FROM suppliers
    WHERE email IS NOT NULL
)
UPDATE suppliers supplier
SET email = NULL
FROM duplicate_emails duplicate
WHERE supplier.id = duplicate.id AND duplicate.duplicate_number > 1;

WITH duplicate_phones AS (
    SELECT id, row_number() OVER (
        PARTITION BY phone ORDER BY created_at NULLS LAST, id
    ) AS duplicate_number
    FROM suppliers
    WHERE phone IS NOT NULL
)
UPDATE suppliers supplier
SET phone = NULL
FROM duplicate_phones duplicate
WHERE supplier.id = duplicate.id AND duplicate.duplicate_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_suppliers_email ON suppliers (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_suppliers_phone ON suppliers (phone) WHERE phone IS NOT NULL;
