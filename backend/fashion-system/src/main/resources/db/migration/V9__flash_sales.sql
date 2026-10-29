-- Flash sale: giá bán riêng cho một số biến thể trong khung giờ, có tổng số suất; mỗi khách tối đa 2 món giá flash
-- cho mỗi biến thể. Suất đã dùng = tổng flash_sale_usages của đơn chưa hủy / chưa bỏ dở thanh toán, nên đơn bị hủy
-- tự trả suất. Chỉ áp dụng cho đơn online; quản lý bằng quyền khuyến mãi (PROMOTION_*).

CREATE TABLE flash_sales (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    starts_at   TIMESTAMP NOT NULL,
    ends_at     TIMESTAMP NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at  TIMESTAMP,
    CONSTRAINT ck_flash_sales_window CHECK (ends_at > starts_at)
);

CREATE INDEX idx_flash_sales_window ON flash_sales (starts_at, ends_at) WHERE active;

CREATE TABLE flash_sale_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flash_sale_id      UUID NOT NULL REFERENCES flash_sales(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id),
    sale_price         NUMERIC(12, 2) NOT NULL CHECK (sale_price > 0),
    quota              INTEGER NOT NULL CHECK (quota > 0),
    CONSTRAINT uq_flash_sale_items_variant UNIQUE (flash_sale_id, product_variant_id)
);

CREATE INDEX idx_flash_sale_items_variant ON flash_sale_items (product_variant_id);

CREATE TABLE flash_sale_usages (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flash_sale_item_id UUID NOT NULL REFERENCES flash_sale_items(id) ON DELETE CASCADE,
    order_id           UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    customer_id        UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    quantity           INTEGER NOT NULL CHECK (quantity > 0),
    created_at         TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_flash_sale_usages_item ON flash_sale_usages (flash_sale_item_id);
CREATE INDEX idx_flash_sale_usages_customer ON flash_sale_usages (customer_id, flash_sale_item_id);
