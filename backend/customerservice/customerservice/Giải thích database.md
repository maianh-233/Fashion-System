# 📘 CUSTOMER DATABASE DESIGN DOCUMENTATION

## 📌 Tổng quan

Database `customer_db` được thiết kế để quản lý:

* Thông tin khách hàng
* Phân hạng khách hàng (tier)
* Địa chỉ giao hàng
* Hệ thống tích điểm (loyalty)
* Log hoạt động người dùng

Thiết kế này hướng tới:

* Khả năng mở rộng (scalability)
* Dễ bảo trì (maintainability)
* Phù hợp kiến trúc microservice

---

# 🧩 1. Bảng `customer_tiers`

## 🎯 Mục đích

Lưu thông tin các cấp độ khách hàng (Silver, Gold, Platinum...)

## 📊 Các cột

| Cột                | Kiểu         | Ý nghĩa                             |
| ------------------ | ------------ | ----------------------------------- |
| `id`               | UUID         | Khóa chính                          |
| `code`             | VARCHAR(50)  | Mã tier (unique, ví dụ: GOLD)       |
| `name`             | VARCHAR(100) | Tên hiển thị                        |
| `min_total_spent`  | DECIMAL      | Tổng chi tiêu tối thiểu để đạt tier |
| `discount_percent` | DECIMAL      | % giảm giá                          |
| `created_at`       | TIMESTAMP    | Thời điểm tạo                       |
| `updated_at`       | TIMESTAMP    | Thời điểm cập nhật                  |

---

# 👤 2. Bảng `customer_profiles`

## 🎯 Mục đích

Lưu thông tin cá nhân của user

## 📊 Các cột

| Cột             | Kiểu      | Ý nghĩa                                     |
| --------------- | --------- | ------------------------------------------- |
| `user_id`       | UUID      | Khóa chính (liên kết user từ auth service)  |
| `full_name`     | VARCHAR   | Họ tên                                      |
| `date_of_birth` | DATE      | Ngày sinh                                   |
| `gender`        | VARCHAR   | Giới tính (MALE, FEMALE, OTHER)             |
| `avatar`        | TEXT      | URL ảnh đại diện                            |
| `created_at`    | TIMESTAMP | Thời điểm tạo                               |
| `updated_at`    | TIMESTAMP | Thời điểm cập nhật                          |

---

# 🏆 3. Bảng `user_tiers`

## 🎯 Mục đích

Lưu lịch sử phân hạng của user

## 📊 Các cột

| Cột           | Kiểu      | Ý nghĩa                       |
| ------------- | --------- | ----------------------------- |
| `id`          | UUID      | Khóa chính                    |
| `user_id`     | UUID      | User                          |
| `tier_id`     | UUID      | Liên kết tới `customer_tiers` |
| `assigned_at` | TIMESTAMP | Thời điểm được gán            |
| `expires_at`  | TIMESTAMP | Thời điểm hết hạn             |
| `note`        | TEXT      | Ghi chú                       |

## ⚠️ Quy tắc

* Mỗi user chỉ có **1 tier active** (`expires_at IS NULL`)

---

# 📍 4. Bảng `customer_addresses`

## 🎯 Mục đích

Lưu nhiều địa chỉ giao hàng của user

## 📊 Các cột

| Cột              | Kiểu      | Ý nghĩa                          |
| ---------------- | --------- | -------------------------------- |
| `id`             | UUID      | Khóa chính                       |
| `user_id`        | UUID      | User                             |
| `receiver_name`  | VARCHAR   | Người nhận                       |
| `receiver_phone` | VARCHAR   | SĐT                              |
| `province`       | VARCHAR   | Tỉnh                             |
| `district`       | VARCHAR   | Quận                             |
| `ward`           | VARCHAR   | Phường                           |
| `address_line`   | TEXT      | Địa chỉ chi tiết                 |
| `postal_code`    | VARCHAR   | Mã bưu điện                      |
| `is_default`     | BOOLEAN   | Địa chỉ mặc định                 |
| `address_type`   | VARCHAR   | Loại địa chỉ (HOME, WORK, OTHER) |
| `created_at`     | TIMESTAMP | Thời điểm tạo                    |
| `updated_at`     | TIMESTAMP | Thời điểm cập nhật               |

## ⚠️ Quy tắc

* Mỗi user chỉ có **1 địa chỉ mặc định**

---

# 💰 5. Bảng `loyalty_accounts`

## 🎯 Mục đích

Lưu trạng thái hiện tại của điểm thưởng

## 📊 Các cột

| Cột              | Kiểu      | Ý nghĩa               |
| ---------------- | --------- | --------------------- |
| `user_id`        | UUID      | Khóa chính            |
| `total_spent`    | DECIMAL   | Tổng tiền đã chi      |
| `points_balance` | INT       | Số điểm hiện tại      |
| `updated_at`     | TIMESTAMP | Lần cập nhật gần nhất |

---

# 🔄 6. Bảng `loyalty_transactions`

## 🎯 Mục đích

Lưu lịch sử thay đổi điểm (immutable log)

## 📊 Các cột

| Cột                | Kiểu      | Ý nghĩa                          |
| ------------------ | --------- | -------------------------------- |
| `id`               | UUID      | Khóa chính                       |
| `user_id`          | UUID      | User                             |
| `transaction_type` | VARCHAR   | Loại giao dịch (EARN, REDEEM...) |
| `points`           | INT       | Số điểm (+ hoặc -)               |
| `reference_type`   | VARCHAR   | Nguồn (ORDER, MANUAL...)         |
| `reference_id`     | UUID      | ID liên quan                     |
| `note`             | TEXT      | Ghi chú                          |
| `created_at`       | TIMESTAMP | Thời điểm tạo                    |

## ⚠️ Đặc điểm

* Không update / delete (immutable)
* Dùng để audit và tính lại điểm nếu cần

---

# 🧾 7. Bảng `customer_activity_logs`

## 🎯 Mục đích

Log hoạt động user trong customer service

## 📊 Các cột

| Cột           | Kiểu      | Ý nghĩa                  |
| ------------- | --------- | ------------------------ |
| `id`          | UUID      | Khóa chính               |
| `user_id`     | UUID      | User thực hiện           |
| `action`      | VARCHAR   | Hành động                |
| `entity_type` | VARCHAR   | Loại dữ liệu bị tác động |
| `entity_id`   | UUID      | ID đối tượng             |
| `metadata`    | JSONB     | Thông tin chi tiết       |
| `created_at`  | TIMESTAMP | Thời điểm log            |


---
