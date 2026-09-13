# Mô hình User / Customer / Department

- `users` chỉ dùng cho nhân viên nội bộ. Quyền của nhân viên đi qua `user_roles`; một nhân viên có thể có nhiều role.
- `customers` là tài khoản khách hàng độc lập, có `id`, username/email/password riêng và không có khóa ngoại tới `users`.
- `departments` và `user_departments` biểu diễn quan hệ nhiều-nhiều giữa nhân viên và phòng ban.
- Các bảng địa chỉ, hạng khách hàng, loyalty, đơn hàng và lượt dùng khuyến mãi đều tham chiếu `customers(id)` bằng `customer_id`.

## Cơ sở dữ liệu mới

Chạy toàn bộ `src/main/java/com/fashionsystem/fashion_system/db/db.sql`.

## Cơ sở dữ liệu đang có dữ liệu

Sao lưu database, sau đó chạy một lần:

```powershell
psql -v ON_ERROR_STOP=1 -d commerce_db -f src/main/java/com/fashionsystem/fashion_system/db/migration/V2__separate_customers_and_departments.sql
```

Migration giữ nguyên UUID và dữ liệu đăng nhập cũ của khách hàng khi chuyển từ `customer_profiles` sang `customers`. Các bản ghi cũ trong `users` chưa bị xóa tự động nhằm tránh mất audit/token; sau khi kiểm tra dữ liệu, công ty có thể lập kế hoạch dọn những tài khoản chỉ mang role `CUSTOMER`.

JWT cũ không có claim `accountType` sẽ không còn hợp lệ. Nhân viên đăng nhập tại `/api/auth/login`; khách hàng đăng nhập tại `/api/auth/login/customer`.

## Mở rộng hồ sơ nhân viên

Sau V2, chạy tiếp `V3__extend_users_for_hr.sql`. Bảng `users` hiện lưu hồ sơ nhân viên cơ bản gồm mã nhân viên, họ tên, ngày sinh, giới tính, ảnh đại diện, chức danh, loại/trạng thái làm việc, ngày vào/nghỉ việc, nơi làm việc và quản lý trực tiếp. `job_title` chỉ là chức danh nhân sự; quyền hệ thống vẫn lấy từ `user_roles`.

Sau V3, chạy `V4__social_auth_and_token_revocation.sql` để tạo liên kết Google/Facebook và danh sách JWT đã logout. Hướng dẫn cấu hình chi tiết nằm trong `SOCIAL_AUTH_GUIDE.md`.

Sau V9, chạy `V10__refresh_token_sessions.sql` để bổ sung refresh session cho cả tài khoản nội bộ và customer. Migration này là bắt buộc trước khi dùng endpoint `/api/auth/refresh`.

Sau V10, chạy `V11__business_audit_logs.sql` để tạo bảng audit nghiệp vụ, index và trigger append-only. Quyền xem log dùng permission có sẵn `LOG_VIEW`.
