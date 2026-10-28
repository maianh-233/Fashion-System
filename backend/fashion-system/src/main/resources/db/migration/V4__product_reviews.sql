-- Product reviews: only customers whose order containing the product is COMPLETED may review it,
-- once per (order, product). Reviews publish immediately; staff can hide them and reply.

CREATE TABLE product_reviews (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id         UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    product_variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    order_id           UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id        UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    rating             SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content            VARCHAR(2000),
    hidden             BOOLEAN NOT NULL DEFAULT false,
    hidden_reason      VARCHAR(500),
    hidden_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    hidden_at          TIMESTAMP,
    reply              VARCHAR(2000),
    replied_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    replied_at         TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at         TIMESTAMP,
    CONSTRAINT uq_product_reviews_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_product_reviews_product_visible ON product_reviews (product_id, created_at DESC) WHERE NOT hidden;
CREATE INDEX idx_product_reviews_customer ON product_reviews (customer_id, created_at DESC);
CREATE INDEX idx_product_reviews_created ON product_reviews (created_at DESC);

CREATE TABLE product_review_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id  UUID NOT NULL REFERENCES product_reviews(id) ON DELETE CASCADE,
    image_url  TEXT NOT NULL,
    public_id  VARCHAR(255),
    sort_order SMALLINT NOT NULL DEFAULT 0 CHECK (sort_order BETWEEN 0 AND 2)
);

CREATE INDEX idx_product_review_images_review ON product_review_images (review_id, sort_order);

-- "Đánh giá sản phẩm" permission group (own sidebar entry) in the CATALOG module.
INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:REVIEW'),1,8) || '-' || substr(md5('permission_group:REVIEW'),9,4) || '-' ||
        substr(md5('permission_group:REVIEW'),13,4) || '-' || substr(md5('permission_group:REVIEW'),17,4) || '-' ||
        substr(md5('permission_group:REVIEW'),21,12))::uuid,
       'Đánh giá sản phẩm', 'REVIEW', 'Xem, ẩn và trả lời đánh giá của khách', current_timestamp, m.id
FROM modules m WHERE m.code = 'CATALOG'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('REVIEW_VIEW', 'Xem đánh giá', 'Xem tất cả đánh giá sản phẩm, kể cả đánh giá đã ẩn'),
    ('REVIEW_MANAGE', 'Quản lý đánh giá', 'Ẩn / hiện đánh giá và trả lời khách')
) AS p(code, name, description)
WHERE g.code = 'REVIEW'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER') AND p.code IN ('REVIEW_VIEW', 'REVIEW_MANAGE'))
   OR (r.code = 'STAFF' AND p.code = 'REVIEW_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
