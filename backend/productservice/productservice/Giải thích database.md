# 📦 Product Service Database Documentation (`product_db`)

Tài liệu này mô tả **chức năng của từng bảng** và **ý nghĩa các thuộc tính (columns)** trong hệ thống `product_service`.

---

# 1. 🏷️ `brands` – Thương hiệu

### Chức năng

Lưu thông tin các thương hiệu sản phẩm (Nike, Adidas, local brand,...)

### Thuộc tính

| Column          | Kiểu      | Ý nghĩa                                |
| --------------- | --------- | -------------------------------------- |
| `id`            | UUID      | Khóa chính                             |
| `name`          | VARCHAR   | Tên thương hiệu                        |
| `code`          | VARCHAR   | Mã thương hiệu (unique, dùng internal) |
| `logo`          | TEXT      | URL logo                               |
| `description`   | TEXT      | Mô tả thương hiệu                      |
| `status`        | VARCHAR   | Trạng thái (ACTIVE, INACTIVE)          |
| `terminated_at` | TIMESTAMP | Thời điểm ngừng hoạt động              |
| `created_at`    | TIMESTAMP | Thời điểm tạo                          |
| `updated_at`    | TIMESTAMP | Thời điểm cập nhật                     |

---

# 2. 🧵 `collections` – Bộ sưu tập

### Chức năng

Nhóm các sản phẩm theo **mùa, năm hoặc concept**

### Thuộc tính

| Column         | Kiểu      | Ý nghĩa                  |
| -------------- | --------- | ------------------------ |
| `id`           | UUID      | Khóa chính               |
| `brand_id`     | UUID      | FK → `brands`            |
| `name`         | VARCHAR   | Tên bộ sưu tập           |
| `code`         | VARCHAR   | Mã bộ sưu tập            |
| `season`       | VARCHAR   | Mùa (Spring, Summer,...) |
| `year`         | INT       | Năm                      |
| `release_date` | DATE      | Ngày ra mắt              |
| `description`  | TEXT      | Mô tả                    |
| `status`       | VARCHAR   | Trạng thái               |
| `created_at`   | TIMESTAMP | Thời điểm tạo            |
| `updated_at`   | TIMESTAMP | Thời điểm cập nhật       |

---

# 3. 🗂️ `categories` – Danh mục

### Chức năng

Tổ chức sản phẩm theo **cấu trúc phân cấp (tree)**

### Thuộc tính

| Column       | Kiểu      | Ý nghĩa                          |
| ------------ | --------- | -------------------------------- |
| `id`         | UUID      | Khóa chính                       |
| `parent_id`  | UUID      | FK self-reference (category cha) |
| `name`       | VARCHAR   | Tên danh mục                     |
| `code`       | VARCHAR   | Mã danh mục                      |
| `created_at` | TIMESTAMP | Thời điểm tạo                    |
| `updated_at` | TIMESTAMP | Thời điểm cập nhật               |

👉 Hỗ trợ cấu trúc:

* Clothing
  └── Men
  └── T-Shirt

---

# 4. 👕 `products` – Sản phẩm

### Chức năng

Lưu thông tin **sản phẩm cha (SPU - Standard Product Unit)**

### Thuộc tính

| Column          | Kiểu      | Ý nghĩa                   |
| --------------- | --------- | ------------------------- |
| `id`            | UUID      | Khóa chính                |
| `brand_id`      | UUID      | FK → `brands`             |
| `collection_id` | UUID      | FK → `collections`        |
| `category_id`   | UUID      | FK → `categories`         |
| `name`          | VARCHAR   | Tên sản phẩm              |
| `slug`          | VARCHAR   | URL-friendly name         |
| `description`   | TEXT      | Mô tả                     |
| `material`      | VARCHAR   | Chất liệu                 |
| `fit`           | VARCHAR   | Form (Slim, Oversize,...) |
| `gender`        | VARCHAR   | Giới tính                 |
| `status`        | VARCHAR   | DRAFT / ACTIVE / ARCHIVED |
| `created_at`    | TIMESTAMP | Thời điểm tạo             |
| `updated_at`    | TIMESTAMP | Thời điểm cập nhật        |

👉 Đây là level **abstract**, chưa có giá cụ thể.

---

# 5. 🎨 `product_variants` – Biến thể sản phẩm

### Chức năng

Đại diện cho **SKU cụ thể (màu + size)**

### Thuộc tính

| Column       | Kiểu      | Ý nghĩa            |
| ------------ | --------- | ------------------ |
| `id`         | UUID      | Khóa chính         |
| `product_id` | UUID      | FK → `products`    |
| `sku`        | VARCHAR   | Mã SKU (unique)    |
| `color`      | VARCHAR   | Màu                |
| `size`       | VARCHAR   | Size               |
| `price`      | DECIMAL   | Giá gốc            |
| `sale_price` | DECIMAL   | Giá giảm           |
| `weight`     | DECIMAL   | Trọng lượng        |
| `barcode`    | VARCHAR   | Mã barcode         |
| `active`     | BOOLEAN   | Có bán hay không   |
| `created_at` | TIMESTAMP | Thời điểm tạo      |
| `updated_at` | TIMESTAMP | Thời điểm cập nhật |

👉 Đây là **đơn vị bán thực tế**

---

# 6. 🖼️ `product_images` – Hình ảnh

### Chức năng

Lưu ảnh cho từng variant

### Thuộc tính

| Column               | Kiểu      | Ý nghĩa                 |
| -------------------- | --------- | ----------------------- |
| `id`                 | UUID      | Khóa chính              |
| `product_variant_id` | UUID      | FK → `product_variants` |
| `image_url`          | TEXT      | Link ảnh                |
| `is_primary`         | BOOLEAN   | Ảnh chính               |
| `sort_order`         | INT       | Thứ tự hiển thị         |
| `created_at`         | TIMESTAMP | Thời điểm tạo           |

---

# 7. ⚙️ `product_attributes` – Thuộc tính mở rộng

### Chức năng

Lưu các thuộc tính linh hoạt (dynamic)

### Thuộc tính

| Column            | Kiểu      | Ý nghĩa                        |
| ----------------- | --------- | ------------------------------ |
| `id`              | UUID      | Khóa chính                     |
| `product_id`      | UUID      | FK → `products`                |
| `attribute_name`  | VARCHAR   | Tên thuộc tính (e.g. "Sleeve") |
| `attribute_value` | VARCHAR   | Giá trị (e.g. "Short")         |
| `created_at`      | TIMESTAMP | Thời điểm tạo                  |

👉 Dùng cho:

* Filter
* Search
* Dynamic schema

---

# 8. 🏷️ `product_tags` – Tag

### Chức năng

Gắn nhãn cho sản phẩm (trend, sale, new,...)

### Thuộc tính

| Column       | Kiểu      | Ý nghĩa       |
| ------------ | --------- | ------------- |
| `id`         | UUID      | Khóa chính    |
| `name`       | VARCHAR   | Tên tag       |
| `created_at` | TIMESTAMP | Thời điểm tạo |

---

# 9. 🔗 `product_tag_mappings` – Mapping tag

### Chức năng

Liên kết **many-to-many giữa products và tags**

### Thuộc tính

| Column       | Kiểu | Ý nghĩa             |
| ------------ | ---- | ------------------- |
| `product_id` | UUID | FK → `products`     |
| `tag_id`     | UUID | FK → `product_tags` |

👉 Composite PK đảm bảo không duplicate

---
