# Tài liệu mô tả database `inventory_service`

## Tổng quan

Database `inventory_db` phục vụ cho `inventory_service` trong hệ thống quản lý thời trang/e-commerce.
Chức năng chính:

* Quản lý cửa hàng/kho
* Quản lý nhân viên kho
* Quản lý nhà cung cấp
* Nhập kho
* Xuất kho
* Theo dõi tồn kho
* Ghi nhận lịch sử biến động kho
* Đặt giữ hàng cho đơn hàng

---

# 1. Bảng `stores`

## Chức năng

Lưu thông tin các cửa hàng hoặc kho hàng trong hệ thống.

Ví dụ:

* Chi nhánh Quận 1
* Kho tổng Bình Tân
* Store online

---

## Các thuộc tính

| Thuộc tính   | Kiểu dữ liệu | Chức năng               |
| ------------ | ------------ | ----------------------- |
| `id`         | UUID         | Khóa chính của cửa hàng |
| `code`       | VARCHAR(50)  | Mã cửa hàng duy nhất    |
| `name`       | VARCHAR(255) | Tên cửa hàng            |
| `address`    | TEXT         | Địa chỉ                 |
| `phone`      | VARCHAR(20)  | Số điện thoại           |
| `active`     | BOOLEAN      | Trạng thái hoạt động    |
| `created_at` | TIMESTAMP    | Thời gian tạo           |
| `updated_at` | TIMESTAMP    | Thời gian cập nhật      |

---

# 2. Bảng `store_staffs`

## Chức năng

Quản lý nhân viên làm việc tại từng cửa hàng/kho.

Một user có thể:

* Làm việc tại nhiều cửa hàng
* Có vai trò khác nhau theo từng cửa hàng

---

## Các thuộc tính

| Thuộc tính   | Kiểu dữ liệu | Chức năng                 |
| ------------ | ------------ | ------------------------- |
| `id`         | UUID         | Khóa chính                |
| `user_id`    | UUID         | ID user từ `auth_service` |
| `store_id`   | UUID         | ID cửa hàng               |
| `staff_role` | VARCHAR(50)  | Vai trò nhân viên         |
| `start_date` | DATE         | Ngày bắt đầu làm việc     |
| `end_date`   | DATE         | Ngày nghỉ việc            |
| `active`     | BOOLEAN      | Trạng thái làm việc       |
| `created_at` | TIMESTAMP    | Thời gian tạo             |

---

# 3. Bảng `suppliers`

## Chức năng

Lưu thông tin nhà cung cấp sản phẩm.

Ví dụ:

* Nhà cung cấp quần áo
* Nhà cung cấp phụ kiện
* Xưởng may

---

## Các thuộc tính

| Thuộc tính     | Kiểu dữ liệu | Chức năng            |
| -------------- | ------------ | -------------------- |
| `id`           | UUID         | Khóa chính           |
| `code`         | VARCHAR(50)  | Mã nhà cung cấp      |
| `name`         | VARCHAR(255) | Tên nhà cung cấp     |
| `contact_name` | VARCHAR(255) | Người liên hệ        |
| `phone`        | VARCHAR(20)  | Số điện thoại        |
| `email`        | VARCHAR(255) | Email                |
| `address`      | TEXT         | Địa chỉ              |
| `status`       | VARCHAR(50)  | Trạng thái hoạt động |
| `created_at`   | TIMESTAMP    | Thời gian tạo        |
| `updated_at`   | TIMESTAMP    | Thời gian cập nhật   |

---

# 4. Bảng `goods_receipts`

## Chức năng

Quản lý phiếu nhập kho.

Phiếu nhập kho dùng khi:

* Nhập hàng từ nhà cung cấp
* Nhập hàng hoàn trả
* Nhập bổ sung tồn kho

---

## Các thuộc tính

| Thuộc tính       | Kiểu dữ liệu  | Chức năng          |
| ---------------- | ------------- | ------------------ |
| `id`             | UUID          | Khóa chính         |
| `receipt_code`   | VARCHAR(50)   | Mã phiếu nhập      |
| `supplier_id`    | UUID          | Nhà cung cấp       |
| `store_id`       | UUID          | Kho/cửa hàng nhập  |
| `received_by`    | UUID          | Người nhận hàng    |
| `approved_by`    | UUID          | Người duyệt phiếu  |
| `receipt_date`   | TIMESTAMP     | Ngày nhập kho      |
| `status`         | VARCHAR(50)   | Trạng thái phiếu   |
| `note`           | TEXT          | Ghi chú            |
| `total_quantity` | INT           | Tổng số lượng nhập |
| `total_amount`   | DECIMAL(14,2) | Tổng tiền nhập     |
| `created_at`     | TIMESTAMP     | Thời gian tạo      |
| `updated_at`     | TIMESTAMP     | Thời gian cập nhật |

---

## Các trạng thái thường dùng

| Status      | Ý nghĩa     |
| ----------- | ----------- |
| `DRAFT`     | Phiếu nháp  |
| `PENDING`   | Chờ duyệt   |
| `APPROVED`  | Đã duyệt    |
| `RECEIVED`  | Đã nhập kho |
| `CANCELLED` | Đã hủy      |

---

# 5. Bảng `goods_receipt_items`

## Chức năng

Lưu danh sách sản phẩm trong phiếu nhập kho.

Một phiếu nhập có nhiều sản phẩm.

---

## Các thuộc tính

| Thuộc tính           | Kiểu dữ liệu  | Chức năng         |
| -------------------- | ------------- | ----------------- |
| `id`                 | UUID          | Khóa chính        |
| `receipt_id`         | UUID          | Phiếu nhập        |
| `product_variant_id` | UUID          | Biến thể sản phẩm |
| `sku`                | VARCHAR(100)  | Mã SKU            |
| `product_name`       | VARCHAR(255)  | Tên sản phẩm      |
| `cost_price`         | DECIMAL(12,2) | Giá nhập          |
| `quantity`           | INT           | Số lượng          |
| `total`              | DECIMAL(14,2) | Tổng tiền dòng    |
| `created_at`         | TIMESTAMP     | Thời gian tạo     |

---

# 6. Bảng `goods_issues`

## Chức năng

Quản lý phiếu xuất kho.

Dùng khi:

* Xuất hàng cho đơn hàng
* Chuyển kho
* Xuất hàng lỗi
* Xuất nội bộ

---

## Các thuộc tính

| Thuộc tính       | Kiểu dữ liệu | Chức năng          |
| ---------------- | ------------ | ------------------ |
| `id`             | UUID         | Khóa chính         |
| `issue_code`     | VARCHAR(50)  | Mã phiếu xuất      |
| `store_id`       | UUID         | Kho xuất           |
| `order_id`       | UUID         | Đơn hàng liên quan |
| `issued_by`      | UUID         | Người xuất kho     |
| `approved_by`    | UUID         | Người duyệt        |
| `issue_type`     | VARCHAR(50)  | Loại xuất kho      |
| `issue_date`     | TIMESTAMP    | Ngày xuất          |
| `status`         | VARCHAR(50)  | Trạng thái         |
| `note`           | TEXT         | Ghi chú            |
| `total_quantity` | INT          | Tổng số lượng xuất |
| `created_at`     | TIMESTAMP    | Thời gian tạo      |
| `updated_at`     | TIMESTAMP    | Thời gian cập nhật |

---

## Các loại xuất kho thường dùng

| Issue Type        | Ý nghĩa           |
| ----------------- | ----------------- |
| `ORDER`           | Xuất cho đơn hàng |
| `TRANSFER`        | Chuyển kho        |
| `DAMAGED`         | Hàng hỏng         |
| `RETURN_SUPPLIER` | Trả nhà cung cấp  |
| `INTERNAL_USE`    | Sử dụng nội bộ    |

---

# 7. Bảng `goods_issue_items`

## Chức năng

Lưu danh sách sản phẩm trong phiếu xuất kho.

---

## Các thuộc tính

| Thuộc tính           | Kiểu dữ liệu | Chức năng         |
| -------------------- | ------------ | ----------------- |
| `id`                 | UUID         | Khóa chính        |
| `issue_id`           | UUID         | Phiếu xuất        |
| `product_variant_id` | UUID         | Biến thể sản phẩm |
| `sku`                | VARCHAR(100) | SKU sản phẩm      |
| `product_name`       | VARCHAR(255) | Tên sản phẩm      |
| `quantity`           | INT          | Số lượng xuất     |
| `created_at`         | TIMESTAMP    | Thời gian tạo     |

---

# 8. Bảng `inventory_transactions`

## Chức năng

Lưu lịch sử biến động tồn kho.

Đây là bảng cực kỳ quan trọng để:

* Audit tồn kho
* Truy vết lịch sử
* Kiểm tra sai lệch kho
* Xây dựng báo cáo tồn kho

---

## Các thuộc tính

| Thuộc tính           | Kiểu dữ liệu | Chức năng             |
| -------------------- | ------------ | --------------------- |
| `id`                 | UUID         | Khóa chính            |
| `product_variant_id` | UUID         | Biến thể sản phẩm     |
| `store_id`           | UUID         | Kho                   |
| `transaction_type`   | VARCHAR(50)  | Loại giao dịch        |
| `reference_type`     | VARCHAR(50)  | Loại chứng từ         |
| `reference_id`       | UUID         | ID chứng từ           |
| `quantity`           | INT          | Số lượng thay đổi     |
| `balance_after`      | INT          | Tồn kho sau giao dịch |
| `created_by`         | UUID         | Người thực hiện       |
| `created_at`         | TIMESTAMP    | Thời gian tạo         |

---

## Ví dụ transaction type

| Type         | Ý nghĩa            |
| ------------ | ------------------ |
| `INBOUND`    | Nhập kho           |
| `OUTBOUND`   | Xuất kho           |
| `RESERVED`   | Giữ hàng           |
| `RELEASED`   | Hủy giữ hàng       |
| `DAMAGED`    | Hàng hỏng          |
| `ADJUSTMENT` | Điều chỉnh tồn kho |

---

# 9. Bảng `inventory_balances`

## Chức năng

Lưu tồn kho hiện tại của từng sản phẩm tại từng cửa hàng/kho.

Đây là bảng đọc nhanh để:

* Hiển thị tồn kho realtime
* Kiểm tra còn hàng
* Đặt hàng nhanh

---

## Các thuộc tính

| Thuộc tính           | Kiểu dữ liệu | Chức năng           |
| -------------------- | ------------ | ------------------- |
| `store_id`           | UUID         | Kho                 |
| `product_variant_id` | UUID         | Biến thể sản phẩm   |
| `available_quantity` | INT          | Số lượng có thể bán |
| `reserved_quantity`  | INT          | Số lượng đang giữ   |
| `damaged_quantity`   | INT          | Số lượng hỏng       |
| `updated_at`         | TIMESTAMP    | Thời gian cập nhật  |

---

## Công thức tồn kho khả dụng

```text
available_quantity = total_stock - reserved_quantity - damaged_quantity
```

---

# 10. Bảng `stock_reservations`

## Chức năng

Quản lý việc giữ hàng cho đơn hàng.

Khi khách đặt hàng:

* Hệ thống giữ hàng
* Tránh oversell
* Chờ thanh toán/xác nhận

---

## Các thuộc tính

| Thuộc tính           | Kiểu dữ liệu | Chức năng           |
| -------------------- | ------------ | ------------------- |
| `id`                 | UUID         | Khóa chính          |
| `order_id`           | UUID         | Đơn hàng            |
| `store_id`           | UUID         | Kho giữ hàng        |
| `product_variant_id` | UUID         | Biến thể sản phẩm   |
| `quantity`           | INT          | Số lượng giữ        |
| `status`             | VARCHAR(30)  | Trạng thái giữ hàng |
| `expired_at`         | TIMESTAMP    | Thời gian hết hạn   |
| `created_at`         | TIMESTAMP    | Thời gian tạo       |
| `updated_at`         | TIMESTAMP    | Thời gian cập nhật  |

---

## Các trạng thái reservation

| Status      | Ý nghĩa     |
| ----------- | ----------- |
| `ACTIVE`    | Đang giữ    |
| `CONFIRMED` | Đã xác nhận |
| `RELEASED`  | Đã hủy giữ  |
| `EXPIRED`   | Hết hạn giữ |

---

# Mối quan hệ giữa các bảng

```text
stores
 ├── store_staffs
 ├── goods_receipts
 ├── goods_issues
 ├── inventory_transactions
 ├── inventory_balances
 └── stock_reservations

suppliers
 └── goods_receipts

goods_receipts
 └── goods_receipt_items

goods_issues
 └── goods_issue_items
```

---

# Luồng hoạt động inventory

## 1. Nhập kho

```text
Supplier -> Goods Receipt -> Inventory Transaction -> Inventory Balance tăng
```

---

## 2. Đặt hàng

```text
Order -> Stock Reservation -> Reserved Quantity tăng
```

---

## 3. Xuất kho

```text
Goods Issue -> Inventory Transaction -> Inventory Balance giảm
```

---

# Ý nghĩa kiến trúc

Thiết kế này giúp hệ thống:

* Dễ audit tồn kho
* Chống oversell
* Hỗ trợ multi-store
* Tối ưu hiệu năng đọc tồn kho
* Theo dõi lịch sử thay đổi
* Dễ mở rộng microservice inventory trong tương lai
