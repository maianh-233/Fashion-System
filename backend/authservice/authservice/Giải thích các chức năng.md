# 📘 Tài liệu mô tả chức năng – Auth Service (fashion-system)

---

## 🎯 1. Mục tiêu của Auth Service

Auth Service chịu trách nhiệm quản lý:

* Xác thực người dùng (Authentication)
* Phân quyền truy cập (Authorization – RBAC)
* Quản lý token (JWT + Refresh Token)
* Ghi log bảo mật và thay đổi dữ liệu (Audit Logging)

---

## 🔐 2. Nhóm chức năng Authentication

### 2.1 Đăng ký (Register)

**Mô tả:**

* Tạo tài khoản mới cho người dùng

**Luồng xử lý:**

1. Nhận email / phone / password
2. Kiểm tra trùng email hoặc phone
3. Hash password (BCrypt)
4. Gán role mặc định (`USER`)
5. Lưu vào DB

**Lưu ý:**

* Có thể yêu cầu xác thực email (tuỳ hệ thống)

---

### 2.2 Đăng nhập (Login)

**Mô tả:**

* Xác thực thông tin đăng nhập và cấp token

**Luồng xử lý:**

1. Tìm user theo email
2. Kiểm tra:

   * `active = true`
   * `locked = false`
   * `deleted_at IS NULL`
3. So sánh password (BCrypt)
4. Nếu đúng:

   * Reset `failed_login_attempts`
   * Cập nhật `last_login`
   * Sinh access token + refresh token
5. Nếu sai:

   * Tăng `failed_login_attempts`

---

### 2.3 Tự động khóa tài khoản (Anti Brute-force)

**Điều kiện:**

* Nếu `failed_login_attempts >= 3`

**Hành động:**

* Set `locked = true`
* Từ chối đăng nhập

---

### 2.4 Tự động đăng nhập (Auto Login / Refresh Token)

**Mô tả:**

* Sử dụng refresh token để cấp lại access token

**Luồng xử lý:**

1. Nhận refresh token
2. Verify token:

   * tồn tại trong DB
   * chưa bị revoke
   * chưa hết hạn
3. Sinh access token mới
4. (Optional) rotate refresh token

---

### 2.5 Đăng xuất (Logout)

**Mô tả:**

* Thu hồi refresh token

**Hành động:**

* Set `revoked_at` cho token

---

### 2.6 Quên mật khẩu (Forgot Password)

**Mô tả:**

* Cho phép reset password

**Luồng xử lý:**

1. Nhập email, mật khẩu
2. Gửi token reset (email hoặc SMS)
3. Người dùng gửi password mới + token
4. Validate token
5. Update password (hash lại)

---

### 2.7 Đổi mật khẩu (Change Password)

**Mô tả:**

* Người dùng đổi password khi đã đăng nhập

**Luồng xử lý:**

1. Xác thực user hiện tại
2. Check password cũ
3. Hash password mới
4. Cập nhật `last_password_change`
5. Revoke toàn bộ refresh token cũ

---

## 👤 3. Quản lý thông tin người dùng

### 3.1 Lấy thông tin user hiện tại (/me)

**Mô tả:**

* Trả về thông tin user đang đăng nhập

**Nguồn dữ liệu:**

* Lấy từ JWT / SecurityContext

---

### 3.2 Cập nhật thông tin cá nhân

**Mô tả:**

* Update email, phone, thông tin cơ bản

**Lưu ý:**

* Không cho phép update các field nhạy cảm (role, permission)

---

## 🛡 4. Authorization (RBAC)

---

### 4.1 Lấy danh sách Permission theo Role

**API:**

* GET `/roles/{id}/permissions`

---

### 4.2 Lấy Permission Group theo Role

**API:**

* GET `/roles/{id}/permission-groups`

---

### 4.3 Tạo Permission Group

**Mô tả:**

* Tạo nhóm quyền (dùng để phân loại)

---

### 4.4 Tạo Permission

**Mô tả:**

* Tạo quyền cụ thể (VD: `USER_CREATE`, `ORDER_READ`)

---

### 4.5 Gán Permission cho Role

**Mô tả:**

* Gán nhiều permission cho một role

---

### 4.6 Tạo Role

**Mô tả:**

* Tạo vai trò mới (ADMIN, STAFF, USER)

---

## 📊 5. Audit Logging (Ghi log hệ thống)

---

### 5.1 Log xác thực (Auth Audit)

**Ghi lại:**

* Login thành công / thất bại
* Logout
* Refresh token

**Thông tin lưu:**

* user_id
* action
* ip_address
* user_agent
* timestamp

---

### 5.2 Log thay đổi quyền (Permission Audit)

**Ghi lại:**

* Thêm / xoá permission khỏi role
* Tạo role / permission

**Thông tin lưu:**

* role_id
* permission_id
* action
* changed_by

---

### 5.3 Log thay đổi dữ liệu người dùng

**Ghi lại:**

* Đổi password
* Update profile
* Reset password

---
