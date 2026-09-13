-- ============================================================================
-- LARGE PRODUCT MASTER DEMO DATA (POSTGRESQL)
-- Product master is GLOBAL. This file intentionally creates NO inventory and
-- NO product_images. products.image_url is an empty string because the current
-- schema requires NOT NULL; upload real images through the Cloudinary API later.
--
-- Volume:
--   20 brands, 30 categories, 40 collections, 300 products,
--   1,800 variants, 1,200 attributes, 30 tags, 1,200 tag mappings.
--
-- Safe to run repeatedly: DEMO_* business keys and deterministic UUIDs are used.
-- ============================================================================

BEGIN;

-- Required by the main schema; harmless when already installed.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- --------------------------------------------------------------------------
-- BRANDS
-- --------------------------------------------------------------------------
WITH brand_names(name, ordinal) AS (
    SELECT * FROM unnest(ARRAY[
        'Lunaria Studio', 'Saigon Heritage', 'Hanoi Minimal', 'Mekong Denim',
        'Lotus Active', 'Indigo District', 'Maison An Nam', 'Urban Nomad',
        'Coastal Linen', 'Velvet Moon', 'Northwind Apparel', 'Daily Form',
        'Noble Thread', 'Sunday Club', 'Aurora Basics', 'Cedar & Silk',
        'Metro Muse', 'Golden Hour', 'Evergreen Wardrobe', 'Studio Twenty'
    ]) WITH ORDINALITY
)
INSERT INTO brands (id, name, code, logo, description, status, created_at, updated_at)
SELECT md5('fashion-demo-brand-' || ordinal)::uuid,
       name,
       'DEMO_BRAND_' || lpad(ordinal::text, 2, '0'),
       NULL,
       'Thương hiệu dữ liệu mẫu số ' || ordinal || ' dùng để kiểm thử Product master.',
       CASE WHEN ordinal IN (18, 20) THEN 'INACTIVE' ELSE 'ACTIVE' END,
       current_timestamp - make_interval(days => (500 - ordinal * 7)::int),
       current_timestamp - make_interval(days => ordinal::int)
FROM brand_names
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, code = EXCLUDED.code, description = EXCLUDED.description,
    status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------------
-- CATEGORIES: 10 roots + 20 children
-- --------------------------------------------------------------------------
WITH roots(ordinal, name, code) AS (
    VALUES
      (1, 'Áo', 'DEMO_CAT_AO'), (2, 'Quần', 'DEMO_CAT_QUAN'),
      (3, 'Váy & Đầm', 'DEMO_CAT_VAY'), (4, 'Áo khoác', 'DEMO_CAT_AO_KHOAC'),
      (5, 'Đồ thể thao', 'DEMO_CAT_THE_THAO'), (6, 'Đồ công sở', 'DEMO_CAT_CONG_SO'),
      (7, 'Đồ mặc nhà', 'DEMO_CAT_MAC_NHA'), (8, 'Phụ kiện', 'DEMO_CAT_PHU_KIEN'),
      (9, 'Giày dép', 'DEMO_CAT_GIAY_DEP'), (10, 'Túi xách', 'DEMO_CAT_TUI_XACH')
)
INSERT INTO categories (id, parent_id, name, code, image_url, created_at, updated_at)
SELECT md5('fashion-demo-category-' || ordinal)::uuid, NULL, name, code, NULL,
       current_timestamp - interval '400 days', current_timestamp
FROM roots
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, code = EXCLUDED.code,
    image_url = EXCLUDED.image_url, updated_at = EXCLUDED.updated_at;

WITH children(ordinal, parent_ordinal, name, code) AS (
    VALUES
      (11,1,'Áo thun','DEMO_CAT_AO_THUN'), (12,1,'Áo sơ mi','DEMO_CAT_AO_SO_MI'),
      (13,2,'Quần jeans','DEMO_CAT_QUAN_JEANS'), (14,2,'Quần tây','DEMO_CAT_QUAN_TAY'),
      (15,3,'Đầm midi','DEMO_CAT_DAM_MIDI'), (16,3,'Chân váy','DEMO_CAT_CHAN_VAY'),
      (17,4,'Áo blazer','DEMO_CAT_BLAZER'), (18,4,'Áo hoodie','DEMO_CAT_HOODIE'),
      (19,5,'Áo tập','DEMO_CAT_AO_TAP'), (20,5,'Quần jogger','DEMO_CAT_JOGGER'),
      (21,6,'Sơ mi công sở','DEMO_CAT_SO_MI_CS'), (22,6,'Quần âu','DEMO_CAT_QUAN_AU'),
      (23,7,'Đồ ngủ','DEMO_CAT_DO_NGU'), (24,7,'Bộ mặc nhà','DEMO_CAT_BO_MAC_NHA'),
      (25,8,'Mũ','DEMO_CAT_MU'), (26,8,'Thắt lưng','DEMO_CAT_THAT_LUNG'),
      (27,9,'Sneaker','DEMO_CAT_SNEAKER'), (28,9,'Sandal','DEMO_CAT_SANDAL'),
      (29,10,'Túi đeo chéo','DEMO_CAT_TUI_DEO_CHEO'), (30,10,'Túi tote','DEMO_CAT_TUI_TOTE')
)
INSERT INTO categories (id, parent_id, name, code, image_url, created_at, updated_at)
SELECT md5('fashion-demo-category-' || ordinal)::uuid,
       md5('fashion-demo-category-' || parent_ordinal)::uuid,
       name, code, NULL, current_timestamp - interval '390 days', current_timestamp
FROM children
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, code = EXCLUDED.code,
    image_url = EXCLUDED.image_url, updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------------
-- COLLECTIONS: two collections per brand
-- --------------------------------------------------------------------------
INSERT INTO collections (
    id, brand_id, name, code, season, year, release_date,
    description, image_url, status, created_at, updated_at
)
SELECT md5('fashion-demo-collection-' || n)::uuid,
       md5('fashion-demo-brand-' || (((n - 1) % 20) + 1))::uuid,
       (ARRAY['Spring Essentials','Summer Escape','Autumn Layers','Winter Edit'])[((n - 1) % 4) + 1]
           || ' ' || (2024 + ((n - 1) % 3)) || ' #' || lpad(n::text, 2, '0'),
       'DEMO_COLLECTION_' || lpad(n::text, 2, '0'),
       (ARRAY['SPRING','SUMMER','AUTUMN','WINTER'])[((n - 1) % 4) + 1],
       2024 + ((n - 1) % 3),
       make_date(2024 + ((n - 1) % 3), (((n - 1) % 12) + 1), 1),
       'Bộ sưu tập mẫu đa phong cách phục vụ kiểm thử tìm kiếm và bộ lọc.',
       NULL,
       CASE WHEN n % 9 = 0 THEN 'DRAFT' ELSE 'ACTIVE' END,
       current_timestamp - make_interval(days => 360 - n * 3), current_timestamp
FROM generate_series(1, 40) AS series(n)
ON CONFLICT (id) DO UPDATE SET
    brand_id = EXCLUDED.brand_id, name = EXCLUDED.name, code = EXCLUDED.code,
    season = EXCLUDED.season, year = EXCLUDED.year, release_date = EXCLUDED.release_date,
    description = EXCLUDED.description, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------------
-- PRODUCTS: 300 records spread across every Brand/Collection/Category
-- --------------------------------------------------------------------------
INSERT INTO products (
    id, brand_id, collection_id, category_id, name, slug, description,
    material, fit, gender, status, created_at, updated_at, image_url
)
SELECT md5('fashion-demo-product-' || n)::uuid,
       md5('fashion-demo-brand-' || (((n - 1) % 20) + 1))::uuid,
       md5('fashion-demo-collection-' || (((n - 1) % 40) + 1))::uuid,
       md5('fashion-demo-category-' || (11 + ((n - 1) % 20)))::uuid,
       (ARRAY['Áo thun','Sơ mi','Quần jeans','Quần tây','Đầm midi','Chân váy',
              'Blazer','Hoodie','Áo thể thao','Jogger','Đồ ngủ','Túi tote'])[((n - 1) % 12) + 1]
           || ' ' ||
       (ARRAY['Essential','Premium','Urban','Classic','Relaxed','Signature'])[((n - 1) % 6) + 1]
           || ' ' || lpad(n::text, 3, '0'),
       'demo-product-' || lpad(n::text, 3, '0'),
       'Sản phẩm dữ liệu mẫu số ' || n || '. Thiết kế phù hợp kiểm thử catalog, filter, pagination và variant.',
       (ARRAY['Cotton 100%','Cotton Compact','Denim','Linen','Polyester tái chế','Rayon','Nỉ da cá','Kaki co giãn'])[((n - 1) % 8) + 1],
       (ARRAY['Regular','Slim','Oversize','Relaxed'])[((n - 1) % 4) + 1],
       (ARRAY['MALE','FEMALE','UNISEX'])[((n - 1) % 3) + 1],
       CASE WHEN n % 17 = 0 THEN 'ARCHIVE' WHEN n % 7 = 0 THEN 'DRAFT' ELSE 'ACTIVE' END,
       current_timestamp - make_interval(days => 300 - (n % 280)),
       current_timestamp - make_interval(hours => n % 240),
       ''
FROM generate_series(1, 300) AS series(n)
ON CONFLICT (id) DO UPDATE SET
    brand_id = EXCLUDED.brand_id, collection_id = EXCLUDED.collection_id,
    category_id = EXCLUDED.category_id, name = EXCLUDED.name, slug = EXCLUDED.slug,
    description = EXCLUDED.description, material = EXCLUDED.material, fit = EXCLUDED.fit,
    gender = EXCLUDED.gender, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------------
-- VARIANTS: six variants per product (1,800 total)
-- --------------------------------------------------------------------------
INSERT INTO product_variants (
    id, product_id, sku, color, size, price, sale_price,
    weight, barcode, active, created_at, updated_at
)
SELECT md5('fashion-demo-variant-' || p || '-' || v)::uuid,
       md5('fashion-demo-product-' || p)::uuid,
       'DEMO-P' || lpad(p::text, 3, '0') || '-' ||
           (ARRAY['BLK','WHT','NVY','BEI','GRN','BRN'])[v] || '-' ||
           (ARRAY['S','M','L','XL','XXL','FS'])[v],
       (ARRAY['Đen','Trắng','Navy','Be','Xanh rêu','Nâu'])[v],
       (ARRAY['S','M','L','XL','XXL','FREESIZE'])[v],
       (199000 + (p % 16) * 50000 + v * 10000)::numeric(12,2),
       CASE WHEN (p + v) % 4 = 0
            THEN (179000 + (p % 16) * 45000 + v * 8000)::numeric(12,2)
            ELSE NULL END,
       (150 + (p % 9) * 45 + v * 10)::numeric(8,2),
       '893' || lpad(p::text, 7, '0') || lpad(v::text, 3, '0'),
       (p + v) % 19 <> 0,
       current_timestamp - make_interval(days => 250 - (p % 230)), current_timestamp
FROM generate_series(1, 300) AS products(p)
CROSS JOIN generate_series(1, 6) AS variants(v)
ON CONFLICT (id) DO UPDATE SET
    product_id = EXCLUDED.product_id, sku = EXCLUDED.sku, color = EXCLUDED.color,
    size = EXCLUDED.size, price = EXCLUDED.price, sale_price = EXCLUDED.sale_price,
    weight = EXCLUDED.weight, barcode = EXCLUDED.barcode, active = EXCLUDED.active,
    updated_at = EXCLUDED.updated_at;

-- --------------------------------------------------------------------------
-- ATTRIBUTES: four text attributes per product (1,200 total)
-- --------------------------------------------------------------------------
INSERT INTO product_attributes (id, product_id, attribute_name, attribute_value, created_at)
SELECT md5('fashion-demo-attribute-' || p || '-' || a)::uuid,
       md5('fashion-demo-product-' || p)::uuid,
       (ARRAY['Phong cách','Hoàn cảnh sử dụng','Hướng dẫn giặt','Xuất xứ'])[a],
       CASE a
           WHEN 1 THEN (ARRAY['Tối giản','Đường phố','Thanh lịch','Năng động'])[((p - 1) % 4) + 1]
           WHEN 2 THEN (ARRAY['Đi làm','Đi chơi','Dự tiệc','Hằng ngày'])[((p - 1) % 4) + 1]
           WHEN 3 THEN (ARRAY['Giặt máy nhẹ','Giặt tay','Không dùng chất tẩy','Giặt ở 30°C'])[((p - 1) % 4) + 1]
           ELSE (ARRAY['Việt Nam','Thái Lan','Hàn Quốc','Nhật Bản'])[((p - 1) % 4) + 1]
       END,
       current_timestamp - make_interval(days => p % 200)
FROM generate_series(1, 300) AS products(p)
CROSS JOIN generate_series(1, 4) AS attributes(a)
ON CONFLICT (id) DO UPDATE SET
    product_id = EXCLUDED.product_id, attribute_name = EXCLUDED.attribute_name,
    attribute_value = EXCLUDED.attribute_value;

-- --------------------------------------------------------------------------
-- TAGS AND PRODUCT MAPPINGS
-- --------------------------------------------------------------------------
WITH tag_names(name, ordinal) AS (
    SELECT * FROM unnest(ARRAY[
        'DEMO New Arrival','DEMO Best Seller','DEMO Limited','DEMO Basic','DEMO Premium',
        'DEMO Streetwear','DEMO Office','DEMO Party','DEMO Casual','DEMO Sport',
        'DEMO Sustainable','DEMO Cotton','DEMO Linen','DEMO Denim','DEMO Oversize',
        'DEMO Slim Fit','DEMO Unisex','DEMO Men','DEMO Women','DEMO Summer',
        'DEMO Winter','DEMO Spring','DEMO Autumn','DEMO Sale','DEMO Trending',
        'DEMO Minimal','DEMO Vintage','DEMO Korean Style','DEMO Local Brand','DEMO Online Exclusive'
    ]) WITH ORDINALITY
)
INSERT INTO product_tags (id, name, created_at)
SELECT md5('fashion-demo-tag-' || ordinal)::uuid, name,
       current_timestamp - make_interval(days => ordinal::int)
FROM tag_names
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

INSERT INTO product_tag_mappings (product_id, tag_id)
SELECT md5('fashion-demo-product-' || p)::uuid,
       md5('fashion-demo-tag-' || (((p * 7 + m * 3) % 30) + 1))::uuid
FROM generate_series(1, 300) AS products(p)
CROSS JOIN generate_series(1, 4) AS mappings(m)
ON CONFLICT (product_id, tag_id) DO NOTHING;

-- Deliberately no INSERT into product_images.

COMMIT;

-- Verification query: expected 20 / 30 / 40 / 300 / 1800 / 1200 / 30 / 1200.
SELECT
  (SELECT count(*) FROM brands WHERE code LIKE 'DEMO_BRAND_%') AS brands,
  (SELECT count(*) FROM categories WHERE code LIKE 'DEMO_CAT_%') AS categories,
  (SELECT count(*) FROM collections WHERE code LIKE 'DEMO_COLLECTION_%') AS collections,
  (SELECT count(*) FROM products WHERE slug LIKE 'demo-product-%') AS products,
  (SELECT count(*) FROM product_variants WHERE sku LIKE 'DEMO-P%') AS variants,
  (SELECT count(*) FROM product_attributes a JOIN products p ON p.id = a.product_id
      WHERE p.slug LIKE 'demo-product-%') AS attributes,
  (SELECT count(*) FROM product_tags WHERE name LIKE 'DEMO %') AS tags,
  (SELECT count(*) FROM product_tag_mappings m JOIN products p ON p.id = m.product_id
      WHERE p.slug LIKE 'demo-product-%') AS tag_mappings;

-- Optional cleanup (run only when demo variants are not referenced by Inventory/orders):
-- BEGIN;
-- DELETE FROM product_tag_mappings WHERE product_id IN (SELECT id FROM products WHERE slug LIKE 'demo-product-%');
-- DELETE FROM product_attributes WHERE product_id IN (SELECT id FROM products WHERE slug LIKE 'demo-product-%');
-- DELETE FROM product_variants WHERE sku LIKE 'DEMO-P%';
-- DELETE FROM products WHERE slug LIKE 'demo-product-%';
-- DELETE FROM collections WHERE code LIKE 'DEMO_COLLECTION_%';
-- DELETE FROM categories WHERE code LIKE 'DEMO_CAT_%';
-- DELETE FROM product_tags WHERE name LIKE 'DEMO %';
-- DELETE FROM brands WHERE code LIKE 'DEMO_BRAND_%';
-- COMMIT;
