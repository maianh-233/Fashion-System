# Order Service Database Documentation

## Tổng quan

Database `order_db` được sử dụng cho microservice `order_service` trong hệ thống Fashion Store.
Chức năng chính:

* Quản lý đơn hàng
* Quản lý sản phẩm trong đơn hàng
* Quản lý địa chỉ giao hàng
* Theo dõi trạng thái đơn hàng
* Quản lý vận chuyển
* Quản lý khuyến mãi áp dụng cho đơn hàng

---

# 1. Bảng `orders`

## Chức năng

Bảng trung tâm dùng để lưu thông tin tổng quát của đơn hàng.

Mỗi record đại diện cho một đơn hàng của khách hàng.

---

## Các thuộc tính

| Thuộc tính       | Kiểu dữ liệu  | Chức năng                                       |
| ---------------- | ------------- | ----------------------------------------------- |
| `id`             | UUID          | Khóa chính của đơn hàng                         |
| `order_code`     | VARCHAR(50)   | Mã đơn hàng duy nhất để hiển thị cho người dùng |
| `user_id`        | UUID          | ID khách hàng tạo đơn                           |
| `store_id`       | UUID          | ID cửa hàng xử lý đơn                           |
| `order_type`     | VARCHAR(20)   | Loại đơn hàng: ONLINE, OFFLINE, PICKUP          |
| `status`         | VARCHAR(50)   | Trạng thái đơn hàng                             |
| `subtotal`       | DECIMAL(14,2) | Tổng tiền sản phẩm trước giảm giá               |
| `discount_total` | DECIMAL(14,2) | Tổng số tiền được giảm                          |
| `tax`            | DECIMAL(14,2) | Thuế của đơn hàng                               |
| `shipping_fee`   | DECIMAL(14,2) | Phí vận chuyển                                  |
| `total_amount`   | DECIMAL(14,2) | Tổng tiền cuối cùng khách cần thanh toán        |
| `payment_status` | VARCHAR(50)   | Trạng thái thanh toán                           |
| `note`           | TEXT          | Ghi chú của khách hàng hoặc nhân viên           |
| `created_at`     | TIMESTAMP     | Thời gian tạo đơn                               |
| `updated_at`     | TIMESTAMP     | Thời gian cập nhật gần nhất                     |

---

## Các trạng thái thường dùng

### `status`

| Giá trị      | Ý nghĩa     |
| ------------ | ----------- |
| `PENDING`    | Đơn mới tạo |
| `CONFIRMED`  | Đã xác nhận |
| `PROCESSING` | Đang xử lý  |
| `SHIPPING`   | Đang giao   |
| `DELIVERED`  | Đã giao     |
| `CANCELLED`  | Đã hủy      |
| `RETURNED`   | Đã hoàn trả |

---

### `payment_status`

| Giá trị    | Ý nghĩa             |
| ---------- | ------------------- |
| `UNPAID`   | Chưa thanh toán     |
| `PAID`     | Đã thanh toán       |
| `REFUNDED` | Đã hoàn tiền        |
| `FAILED`   | Thanh toán thất bại |

---

# 2. Bảng `order_items`

## Chức năng

Lưu danh sách sản phẩm thuộc một đơn hàng.

Một đơn hàng có thể chứa nhiều sản phẩm.

---

## Các thuộc tính

| Thuộc tính           | Kiểu dữ liệu  | Chức năng                           |
| -------------------- | ------------- | ----------------------------------- |
| `id`                 | UUID          | Khóa chính                          |
| `order_id`           | UUID          | ID đơn hàng                         |
| `product_id`         | UUID          | ID sản phẩm                         |
| `product_variant_id` | UUID          | ID biến thể sản phẩm                |
| `product_name`       | VARCHAR(255)  | Tên sản phẩm tại thời điểm đặt hàng |
| `sku`                | VARCHAR(100)  | Mã SKU sản phẩm                     |
| `color`              | VARCHAR(100)  | Màu sản phẩm                        |
| `size`               | VARCHAR(50)   | Kích thước sản phẩm                 |
| `image_url`          | TEXT          | Ảnh sản phẩm                        |
| `price`              | DECIMAL(12,2) | Giá của 1 sản phẩm                  |
| `quantity`           | INT           | Số lượng mua                        |
| `total`              | DECIMAL(14,2) | Tổng tiền của item                  |
| `created_at`         | TIMESTAMP     | Thời gian tạo                       |

---

## Công thức tính

```text
total = price * quantity
```

---

# 3. Bảng `order_addresses`

## Chức năng

Lưu địa chỉ giao hàng của đơn hàng.

Mỗi đơn hàng chỉ có một địa chỉ giao hàng.

---

## Các thuộc tính

| Thuộc tính       | Kiểu dữ liệu | Chức năng                |
| ---------------- | ------------ | ------------------------ |
| `id`             | UUID         | Khóa chính               |
| `order_id`       | UUID         | ID đơn hàng              |
| `receiver_name`  | VARCHAR(255) | Tên người nhận           |
| `receiver_phone` | VARCHAR(20)  | Số điện thoại người nhận |
| `province`       | VARCHAR(100) | Tỉnh/Thành phố           |
| `district`       | VARCHAR(100) | Quận/Huyện               |
| `ward`           | VARCHAR(100) | Phường/Xã                |
| `address_line`   | TEXT         | Địa chỉ chi tiết         |
| `postal_code`    | VARCHAR(20)  | Mã bưu điện              |
| `address_type`   | VARCHAR(20)  | Loại địa chỉ             |
| `created_at`     | TIMESTAMP    | Thời gian tạo            |

---

## `address_type`

| Giá trị    | Ý nghĩa            |
| ---------- | ------------------ |
| `SHIPPING` | Địa chỉ giao hàng  |
| `BILLING`  | Địa chỉ thanh toán |

---

# 4. Bảng `order_status_histories`

## Chức năng

Lưu lịch sử thay đổi trạng thái đơn hàng.

Giúp audit và tracking luồng xử lý đơn hàng.

---

## Các thuộc tính

| Thuộc tính    | Kiểu dữ liệu | Chức năng          |
| ------------- | ------------ | ------------------ |
| `id`          | UUID         | Khóa chính         |
| `order_id`    | UUID         | ID đơn hàng        |
| `from_status` | VARCHAR(50)  | Trạng thái cũ      |
| `to_status`   | VARCHAR(50)  | Trạng thái mới     |
| `changed_by`  | UUID         | ID người thay đổi  |
| `note`        | TEXT         | Ghi chú thay đổi   |
| `changed_at`  | TIMESTAMP    | Thời gian thay đổi |

---

## Ví dụ

| from_status | to_status  |
| ----------- | ---------- |
| PENDING     | CONFIRMED  |
| CONFIRMED   | PROCESSING |
| PROCESSING  | SHIPPING   |
| SHIPPING    | DELIVERED  |

---

# 5. Bảng `shipments`

## Chức năng

Quản lý thông tin vận chuyển của đơn hàng.

Một đơn hàng chỉ có một shipment.

---

## Các thuộc tính

| Thuộc tính          | Kiểu dữ liệu | Chức năng                            |
| ------------------- | ------------ | ------------------------------------ |
| `id`                | UUID         | Khóa chính                           |
| `order_id`          | UUID         | ID đơn hàng                          |
| `shipping_provider` | VARCHAR(100) | Đơn vị vận chuyển                    |
| `tracking_code`     | VARCHAR(100) | Mã vận đơn                           |
| `shipping_status`   | VARCHAR(50)  | Trạng thái giao hàng                 |
| `shipped_at`        | TIMESTAMP    | Thời gian giao cho đơn vị vận chuyển |
| `delivered_at`      | TIMESTAMP    | Thời gian giao thành công            |
| `created_at`        | TIMESTAMP    | Thời gian tạo                        |
| `updated_at`        | TIMESTAMP    | Thời gian cập nhật                   |

---

## `shipping_status`

| Giá trị      | Ý nghĩa         |
| ------------ | --------------- |
| `PENDING`    | Chờ giao        |
| `PICKED_UP`  | Đã lấy hàng     |
| `IN_TRANSIT` | Đang vận chuyển |
| `DELIVERED`  | Giao thành công |
| `FAILED`     | Giao thất bại   |
| `RETURNED`   | Hoàn hàng       |

---

# 6. Bảng `order_promotions`

## Chức năng

Lưu các chương trình khuyến mãi áp dụng cho đơn hàng.

Một đơn hàng có thể áp dụng nhiều promotion.

---

## Các thuộc tính

| Thuộc tính        | Kiểu dữ liệu  | Chức năng                  |
| ----------------- | ------------- | -------------------------- |
| `id`              | UUID          | Khóa chính                 |
| `order_id`        | UUID          | ID đơn hàng                |
| `promotion_id`    | UUID          | ID chương trình khuyến mãi |
| `promotion_code`  | VARCHAR(50)   | Mã giảm giá                |
| `discount_amount` | DECIMAL(14,2) | Số tiền được giảm          |
| `created_at`      | TIMESTAMP     | Thời gian tạo              |

---

# 7. Quan hệ giữa các bảng

```text
orders
 ├── order_items
 ├── order_addresses
 ├── order_status_histories
 ├── shipments
 └── order_promotions
```

---

# 8. Ý nghĩa các INDEX

## Chức năng

Các index giúp tăng tốc độ truy vấn dữ liệu.

---

## Danh sách index

| Index                                 | Chức năng                    |
| ------------------------------------- | ---------------------------- |
| `idx_orders_user_id`                  | Tìm đơn hàng theo user       |
| `idx_orders_store_id`                 | Tìm đơn hàng theo cửa hàng   |
| `idx_orders_status`                   | Tìm đơn theo trạng thái      |
| `idx_order_items_order_id`            | Lấy danh sách item của order |
| `idx_order_items_variant_id`          | Tìm item theo variant        |
| `idx_order_status_histories_order_id` | Lấy lịch sử trạng thái       |
| `idx_shipments_order_id`              | Tìm shipment theo order      |
| `idx_order_promotions_order_id`       | Lấy promotion của order      |

---

# 9. Luồng xử lý đơn hàng

```text
Khách đặt hàng
    ↓
Tạo orders
    ↓
Tạo order_items
    ↓
Tạo order_addresses
    ↓
Áp dụng promotions
    ↓
Thanh toán
    ↓
Tạo shipment
    ↓
Cập nhật trạng thái đơn hàng
    ↓
Lưu lịch sử trạng thái
```

---

# 10. Đề xuất cải tiến database

## Foreign Key

Hiện tại chưa có foreign key.

Nên bổ sung:

```sql
ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_order
FOREIGN KEY (order_id) REFERENCES orders(id);
```

Tương tự cho:

* order_addresses
* order_status_histories
* shipments
* order_promotions

---

## Enum cho trạng thái

Nên dùng ENUM thay vì VARCHAR để:

* Giảm lỗi dữ liệu
* Tăng tính nhất quán
* Dễ validate

Ví dụ:

```sql
CREATE TYPE order_status_enum AS ENUM (
  'PENDING',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPING',
  'DELIVERED',
  'CANCELLED',
  'RETURNED'
);
```

---

# 11. Kiến trúc nghiệp vụ

## order_service chịu trách nhiệm

* Tạo đơn hàng
* Quản lý trạng thái đơn hàng
* Tracking shipment
* Quản lý item trong order
* Áp dụng promotion
* Audit lịch sử thay đổi trạng thái


