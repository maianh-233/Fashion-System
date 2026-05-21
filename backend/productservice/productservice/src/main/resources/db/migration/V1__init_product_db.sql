/* =========================================================
   PRODUCT SERVICE - INITIAL SCHEMA
   ========================================================= */

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- 1. brands
-- =========================
CREATE TABLE brands (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  code VARCHAR(100) UNIQUE,
  logo TEXT,
  description TEXT,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  terminated_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
);

-- =========================
-- 2. collections
-- =========================
CREATE TABLE collections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  brand_id UUID,
  name VARCHAR(255) NOT NULL,
  code VARCHAR(100) UNIQUE,
  season VARCHAR(50),
  year INT,
  release_date DATE,
  description TEXT,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT fk_collections_brand
    FOREIGN KEY (brand_id) REFERENCES brands(id)
);

-- =========================
-- 3. categories
-- =========================
CREATE TABLE categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  parent_id UUID,
  name VARCHAR(255) NOT NULL,
  code VARCHAR(100) UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT fk_categories_parent
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- =========================
-- 4. products
-- =========================
CREATE TABLE products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  brand_id UUID,
  collection_id UUID,
  category_id UUID,
  name VARCHAR(255) NOT NULL,
  slug VARCHAR(255) UNIQUE,
  description TEXT,
  material VARCHAR(255),
  fit VARCHAR(100),
  gender VARCHAR(20),
  status VARCHAR(50) DEFAULT 'DRAFT',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  image_url TEXT NOT NULL,
  CONSTRAINT fk_products_brand
    FOREIGN KEY (brand_id) REFERENCES brands(id),
  CONSTRAINT fk_products_collection
    FOREIGN KEY (collection_id) REFERENCES collections(id),
  CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- =========================
-- 5. product_variants
-- =========================
CREATE TABLE product_variants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL,
  sku VARCHAR(100) UNIQUE NOT NULL,
  color VARCHAR(100),
  size VARCHAR(50),
  price DECIMAL(12,2) NOT NULL,
  sale_price DECIMAL(12,2),
  weight DECIMAL(8,2),
  barcode VARCHAR(100),
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  CONSTRAINT fk_variants_product
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =========================
-- 6. product_images
-- =========================
CREATE TABLE product_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_variant_id UUID NOT NULL,
  image_url TEXT NOT NULL,
  is_primary BOOLEAN DEFAULT FALSE,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_images_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

-- =========================
-- 7. product_attributes
-- =========================
CREATE TABLE product_attributes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL,
  attribute_name VARCHAR(100) NOT NULL,
  attribute_value VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attributes_product
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =========================
-- 8. product_tags
-- =========================
CREATE TABLE product_tags (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- 9. product_tag_mappings
-- =========================
CREATE TABLE product_tag_mappings (
  product_id UUID NOT NULL,
  tag_id UUID NOT NULL,
  PRIMARY KEY (product_id, tag_id),
  CONSTRAINT fk_tagmap_product
    FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_tagmap_tag
    FOREIGN KEY (tag_id) REFERENCES product_tags(id)
);

-- =========================
-- INDEXES
-- =========================
CREATE INDEX idx_collections_brand_id ON collections(brand_id);
CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_collection_id ON products(collection_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_product_images_variant_id ON product_images(product_variant_id);
CREATE INDEX idx_product_attributes_product_id ON product_attributes(product_id);