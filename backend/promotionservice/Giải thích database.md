# promotion_service Database Documentation

````md
# Promotion Service Database Documentation

## Tổng quan

`promotion_service` quản lý toàn bộ dữ liệu liên quan đến:
- Mã giảm giá (Promotion/Coupon)
- Điều kiện áp dụng khuyến mãi
- Giới hạn sử dụng
- Lịch sử sử dụng promotion
- Áp dụng promotion cho:
  - sản phẩm
  - category
  - brand
  - collection
  - membership tier

---

# 1. Table: promotions

## Mục đích
Lưu thông tin chính của chương trình khuyến mãi hoặc mã giảm giá.

---

## Cấu trúc bảng

| Column | Type | Mô tả |
|---|---|---|
| id | UUID | Khóa chính của promotion |
| code | VARCHAR(50) | Mã giảm giá duy nhất |
| name | VARCHAR(255) | Tên chương trình khuyến mãi |
| discount_type | VARCHAR(20) | Loại giảm giá |
| discount_value | DECIMAL(10,2) | Giá trị giảm |
| start_date | TIMESTAMP | Thời gian bắt đầu |
| end_date | TIMESTAMP | Thời gian kết thúc |
| min_order_value | DECIMAL(14,2) | Giá trị đơn hàng tối thiểu |
| max_discount | DECIMAL(14,2) | Giảm tối đa |
| usage_limit | INT | Tổng số lượt sử dụng tối đa |
| usage_per_user | INT | Số lần mỗi user được sử dụng |
| active | BOOLEAN | Promotion còn hoạt động hay không |
| created_at | TIMESTAMP | Thời gian tạo |
| updated_at | TIMESTAMP | Thời gian cập nhật |

---

## Giải thích thuộc tính

### id
- UUID định danh promotion
- Sử dụng `gen_random_uuid()`

Ví dụ:
```text
550e8400-e29b-41d4-a716-446655440000
````

---

### code

Mã coupon user nhập khi thanh toán.

Ví dụ:

```text
SUMMER2026
WELCOME10
```

* UNIQUE
* Không được null

---

### name

Tên hiển thị của chương trình khuyến mãi.

Ví dụ:

```text
Summer Sale 2026
Giảm giá thành viên VIP
```

---

### discount_type

Xác định loại giảm giá.

Ví dụ:

| Value   | Ý nghĩa              |
| ------- | -------------------- |
| PERCENT | Giảm theo %          |
| FIXED   | Giảm số tiền cố định |

---

### discount_value

Giá trị giảm.

Ví dụ:

* `10` → giảm 10%
* `50000` → giảm 50.000 VNĐ

---

### start_date / end_date

Khoảng thời gian promotion có hiệu lực.

Ví dụ:

```text
2026-05-01 00:00:00
2026-05-31 23:59:59
```

---

### min_order_value

Giá trị đơn hàng tối thiểu để áp dụng.

Ví dụ:

```text
500000
```

→ Đơn hàng phải từ 500k mới dùng được.

---

### max_discount

Giới hạn số tiền giảm tối đa.

Ví dụ:

```text
100000
```

Nếu giảm 20% nhưng vượt quá 100k thì chỉ giảm tối đa 100k.

---

### usage_limit

Tổng số lượt sử dụng toàn hệ thống.

Ví dụ:

```text
1000
```

Promotion sẽ hết hiệu lực sau 1000 lượt dùng.

---

### usage_per_user

Giới hạn số lần mỗi user sử dụng.

Ví dụ:

```text
1
```

→ Mỗi user chỉ dùng được 1 lần.

---

### active

Trạng thái hoạt động.

| Value | Ý nghĩa         |
| ----- | --------------- |
| true  | Đang hoạt động  |
| false | Ngưng hoạt động |

---

### created_at / updated_at

Quản lý thời gian tạo và cập nhật dữ liệu.

---

# 2. Table: promotion_tiers

## Mục đích

Liên kết promotion với membership tier.

Ví dụ:

* VIP
* GOLD
* SILVER

---

## Cấu trúc bảng

| Column       | Type | Mô tả        |
| ------------ | ---- | ------------ |
| promotion_id | UUID | ID promotion |
| tier_id      | UUID | ID tier      |

---

## Ý nghĩa

Cho phép:

* Một promotion áp dụng cho nhiều tier
* Một tier có nhiều promotion

Đây là bảng Many-to-Many.

---

# 3. Table: promotion_products

## Mục đích

Promotion áp dụng riêng cho sản phẩm cụ thể.

---

## Cấu trúc bảng

| Column       | Type | Mô tả        |
| ------------ | ---- | ------------ |
| promotion_id | UUID | ID promotion |
| product_id   | UUID | ID sản phẩm  |

---

## Ý nghĩa

Ví dụ:

* Giảm giá riêng cho áo hoodie
* Flash sale cho sản phẩm cụ thể

Quan hệ Many-to-Many.

---

# 4. Table: promotion_categories

## Mục đích

Promotion áp dụng cho category.

---

## Cấu trúc bảng

| Column       | Type | Mô tả        |
| ------------ | ---- | ------------ |
| promotion_id | UUID | ID promotion |
| category_id  | UUID | ID category  |

---

## Ví dụ

Promotion áp dụng cho:

* Áo thun
* Quần jean
* Giày sneaker

---

# 5. Table: promotion_brands

## Mục đích

Promotion áp dụng theo brand.

---

## Cấu trúc bảng

| Column       | Type | Mô tả        |
| ------------ | ---- | ------------ |
| promotion_id | UUID | ID promotion |
| brand_id     | UUID | ID brand     |

---

## Ví dụ

Giảm giá cho:

* Nike
* Adidas
* Puma

---

# 6. Table: promotion_collections

## Mục đích

Promotion áp dụng theo collection.

---

## Cấu trúc bảng

| Column        | Type | Mô tả         |
| ------------- | ---- | ------------- |
| promotion_id  | UUID | ID promotion  |
| collection_id | UUID | ID collection |

---

## Ví dụ

Áp dụng cho:

* Summer Collection 2026
* Winter Collection
* Limited Edition

---

# 7. Table: promotion_usages

## Mục đích

Lưu lịch sử sử dụng promotion.

Dùng để:

* kiểm tra số lượt dùng
* chống spam coupon
* thống kê promotion

---

## Cấu trúc bảng

| Column       | Type      | Mô tả                  |
| ------------ | --------- | ---------------------- |
| id           | UUID      | ID lịch sử sử dụng     |
| promotion_id | UUID      | Promotion được sử dụng |
| order_id     | UUID      | Đơn hàng áp dụng       |
| user_id      | UUID      | User sử dụng           |
| used_at      | TIMESTAMP | Thời gian sử dụng      |

---

## Giải thích thuộc tính

### promotion_id

Promotion nào đã được sử dụng.

---

### order_id

Đơn hàng áp dụng promotion.

---

### user_id

Người dùng đã dùng promotion.

Có thể NULL nếu:

* guest checkout
* không đăng nhập

---

### used_at

Thời gian sử dụng coupon.

---

# 8. Table: promotion_conditions

## Mục đích

Lưu điều kiện động của promotion.

Cho phép hệ thống mở rộng rule mà không cần sửa schema.

---

## Cấu trúc bảng

| Column          | Type         | Mô tả               |
| --------------- | ------------ | ------------------- |
| id              | UUID         | ID condition        |
| promotion_id    | UUID         | Promotion liên quan |
| condition_type  | VARCHAR(50)  | Loại điều kiện      |
| condition_value | VARCHAR(255) | Giá trị điều kiện   |
| created_at      | TIMESTAMP    | Thời gian tạo       |

---

## Ví dụ condition_type

| condition_type  | Ý nghĩa                     |
| --------------- | --------------------------- |
| MIN_QUANTITY    | Số lượng sản phẩm tối thiểu |
| FIRST_ORDER     | Chỉ áp dụng đơn đầu tiên    |
| PAYMENT_METHOD  | Phương thức thanh toán      |
| SHIPPING_METHOD | Phương thức vận chuyển      |

---

## Ví dụ dữ liệu

| condition_type | condition_value |
| -------------- | --------------- |
| MIN_QUANTITY   | 3               |
| PAYMENT_METHOD | VNPAY           |
| FIRST_ORDER    | true            |

---

# Indexes

## idx_promotion_usages_promotion_id

Tăng tốc:

```sql
WHERE promotion_id = ?
```

---

## idx_promotion_usages_order_id

Tăng tốc truy vấn:

```sql
WHERE order_id = ?
```

---

## idx_promotion_usages_user_id

Tăng tốc:

```sql
WHERE user_id = ?
```

Dùng để kiểm tra:

* user đã dùng coupon chưa
* số lần user đã dùng

---

## idx_promotion_conditions_promotion_id

Tăng tốc lấy danh sách điều kiện của promotion.

---

# Quan hệ giữa các bảng

## promotions

Là bảng trung tâm.

Quan hệ:

* 1-N với `promotion_usages`
* 1-N với `promotion_conditions`
* N-N với:

  * promotion_products
  * promotion_categories
  * promotion_brands
  * promotion_collections
  * promotion_tiers

---

# Luồng hoạt động promotion

## Khi user nhập coupon

Hệ thống sẽ:

1. Tìm promotion theo `code`
2. Kiểm tra:

   * active
   * thời gian hiệu lực
   * usage_limit
   * usage_per_user
3. Kiểm tra điều kiện:

   * min_order_value
   * conditions
4. Kiểm tra đối tượng áp dụng:

   * product
   * category
   * brand
   * collection
   * tier
5. Tính discount
6. Lưu vào `promotion_usages`

---

# Đề xuất cải tiến

## Nên thêm Foreign Key

Hiện tại schema chưa có FK.

Ví dụ:

```sql
ALTER TABLE promotion_usages
ADD CONSTRAINT fk_promotion_usages_promotion
FOREIGN KEY (promotion_id)
REFERENCES promotions(id);
```

---

## Nên thêm ENUM cho discount_type

Hiện tại dùng VARCHAR dễ sai dữ liệu.

Nên dùng:

```sql
PERCENT
FIXED
```

---

## Nên thêm trạng thái promotion

Ví dụ:

```text
DRAFT
ACTIVE
EXPIRED
DISABLED
```

Thay vì chỉ dùng boolean `active`.


