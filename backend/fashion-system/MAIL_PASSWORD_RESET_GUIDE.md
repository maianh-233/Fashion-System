# Email và quên mật khẩu

## Cấu hình

Sao chép các biến `MAIL_*` và `PASSWORD_RESET_*` từ `.env.example` sang `.env`, sau đó điền thông tin SMTP thật. Không commit `.env`.

Với database đã tồn tại, chạy migration:

```powershell
psql -v ON_ERROR_STOP=1 -d commerce_db -f src/main/java/com/fashionsystem/fashion_system/db/migration/V5__password_reset_otps.sql
```

Database cài mới dùng trực tiếp `src/main/java/com/fashionsystem/fashion_system/db/db.sql`.

## API

`accountType` nhận `EMPLOYEE` hoặc `CUSTOMER`.

### 1. Gửi OTP / gửi lại OTP

```http
POST /api/auth/password/request-otp
Content-Type: application/json

{"email":"customer@example.com","accountType":"CUSTOMER"}
```

Gửi lại dùng cùng body tại `POST /api/auth/password/resend-otp`. OTP cũ sẽ bị vô hiệu hóa.

### 2. Xác minh OTP

```http
POST /api/auth/password/verify-otp
Content-Type: application/json

{"email":"customer@example.com","accountType":"CUSTOMER","otp":"123456"}
```

Response chứa `resetToken` ngắn hạn. OTP không được dùng trực tiếp để đổi mật khẩu.

### 3. Đặt mật khẩu mới

```http
POST /api/auth/password/reset
Content-Type: application/json

{"resetToken":"token-tu-buoc-2","newPassword":"new-password"}
```

## Mail helper

Inject `MailHelper` và dùng một trong các hàm:

- `sendText(to, subject, content)`
- `sendHtml(to, subject, html)`
- `sendOtp(to, recipientName, otp, expirationMinutes)`
- `sendWithAttachments(to, subject, html, List<Path>)`

Thông tin SMTP, địa chỉ gửi, tên người gửi và tiêu đề OTP đều lấy từ environment.
