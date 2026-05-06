# 📘 Tài liệu mô tả Database `auth_db`

Tài liệu này giải thích **chức năng của từng bảng** và **ý nghĩa các thuộc tính** trong hệ thống xác thực & phân quyền (Authentication + Authorization).

---

# 🧩 1. Bảng `users` – Quản lý người dùng

## 🎯 Mục đích

Lưu thông tin tài khoản và trạng thái xác thực của người dùng.

## 📌 Thuộc tính

| Trường                  | Ý nghĩa                   |
| ----------------------- | ------------------------- |
| `id`                    | UUID định danh duy nhất   |
| `email`                 | Email đăng nhập (unique)  |
| `phone`                 | Số điện thoại (unique)    |
| `password_hash`         | Mật khẩu đã mã hóa        |
| `active`                | Trạng thái hoạt động      |
| `locked`                | Tài khoản bị khóa         |
| `failed_login_attempts` | Số lần đăng nhập sai      |
| `last_password_change`  | Lần đổi mật khẩu gần nhất |
| `email_verified`        | Đã xác thực email         |
| `phone_verified`        | Đã xác thực số điện thoại |
| `last_login`            | Lần đăng nhập gần nhất    |
| `created_at`            | Thời điểm tạo             |
| `updated_at`            | Thời điểm cập nhật        |
| `deleted_at`            | Soft delete               |

---

# 🧩 2. Bảng `roles` – Vai trò

## 🎯 Mục đích

Định nghĩa các vai trò trong hệ thống (RBAC).

## 📌 Thuộc tính

| Trường        | Ý nghĩa             |
| ------------- | ------------------- |
| `id`          | UUID                |
| `code`        | Mã role (VD: ADMIN) |
| `name`        | Tên role            |
| `description` | Mô tả               |
| `created_at`  | Thời gian tạo       |

---

# 🧩 3. Bảng `user_roles` – Gán vai trò cho user

## 🎯 Mục đích

Thiết lập quan hệ nhiều-nhiều giữa user và role.

## 📌 Thuộc tính

| Trường        | Ý nghĩa       |
| ------------- | ------------- |
| `user_id`     | ID user       |
| `role_id`     | ID role       |
| `assigned_at` | Thời điểm gán |

---

# 🧩 4. Bảng `permission_groups` – Nhóm quyền

## 🎯 Mục đích

Phân nhóm các quyền theo module/chức năng.

## 📌 Thuộc tính

| Trường        | Ý nghĩa       |
| ------------- | ------------- |
| `id`          | UUID          |
| `name`        | Tên nhóm      |
| `code`        | Mã nhóm       |
| `description` | Mô tả         |
| `created_at`  | Thời gian tạo |

---

# 🧩 5. Bảng `permissions` – Quyền

## 🎯 Mục đích

Định nghĩa các quyền cụ thể trong hệ thống.

## 📌 Thuộc tính

| Trường        | Ý nghĩa                    |
| ------------- | -------------------------- |
| `id`          | UUID                       |
| `name`        | Tên quyền                  |
| `code`        | Mã quyền (VD: USER_CREATE) |
| `group_id`    | Thuộc nhóm quyền           |
| `description` | Mô tả                      |
| `created_at`  | Thời gian tạo              |

---

# 🧩 6. Bảng `role_permissions` – Gán quyền cho role

## 🎯 Mục đích

Liên kết role với các permission.

## 📌 Thuộc tính

| Trường          | Ý nghĩa       |
| --------------- | ------------- |
| `role_id`       | ID role       |
| `permission_id` | ID permission |

---

# 🧩 7. Bảng `user_permissions` – Gán quyền trực tiếp

## 🎯 Mục đích

Cấp quyền riêng lẻ cho user (override role).

## 📌 Thuộc tính

| Trường          | Ý nghĩa       |
| --------------- | ------------- |
| `user_id`       | ID user       |
| `permission_id` | ID permission |

---

# 🧩 8. Bảng `user_tokens` – Quản lý token

## 🎯 Mục đích

Quản lý refresh token và session người dùng.

## 📌 Thuộc tính

| Trường                 | Ý nghĩa                         |
| ---------------------- | ------------------------------- |
| `id`                   | UUID                            |
| `user_id`              | Chủ sở hữu token                |
| `token_hash`           | Hash của token                  |
| `token_type`           | Loại token                      |
| `refresh_token_family` | Nhóm token (chống reuse attack) |
| `parent_token_id`      | Token trước đó                  |
| `expires_at`           | Thời gian hết hạn               |
| `revoked_at`           | Thời điểm thu hồi               |
| `device`               | Thiết bị                        |
| `ip_address`           | IP                              |
| `user_agent`           | Trình duyệt                     |
| `created_at`           | Thời gian tạo                   |

---

# 🧩 9. Bảng `role_permission_audit` – Log thay đổi quyền

## 🎯 Mục đích

Ghi lại lịch sử thay đổi quyền của role.

## 📌 Thuộc tính

| Trường          | Ý nghĩa                |
| --------------- | ---------------------- |
| `id`            | UUID                   |
| `role_id`       | Role bị thay đổi       |
| `permission_id` | Permission liên quan   |
| `action`        | Hành động (ADD/REMOVE) |
| `changed_by`    | Người thực hiện        |
| `changed_at`    | Thời điểm              |

---

# 🧩 10. Bảng `auth_audit_logs` – Log xác thực

## 🎯 Mục đích

Theo dõi hoạt động đăng nhập và bảo mật.

## 📌 Thuộc tính

| Trường        | Ý nghĩa                      |
| ------------- | ---------------------------- |
| `id`          | UUID                         |
| `user_id`     | User thực hiện               |
| `action`      | Hành động (LOGIN, LOGOUT...) |
| `description` | Mô tả                        |
| `ip_address`  | IP                           |
| `user_agent`  | Trình duyệt                  |
| `created_at`  | Thời gian                    |

---