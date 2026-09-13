# Đăng ký/đăng nhập khách hàng bằng Google

Backend dùng mô hình **token handoff** phù hợp web SPA/mobile:

1. Frontend mở Google Identity Services.
2. Nhà cung cấp trả credential cho frontend.
3. Frontend gửi credential qua HTTPS đến `POST /api/auth/login/customer/social`.
4. Backend tự xác minh credential với Google, không tin email hay tên do frontend tự gửi.
5. Backend tìm `customer_social_accounts`. Nếu chưa có, backend tạo `customers` và liên kết social; nếu đã có, backend đăng nhập customer tương ứng.
6. Backend trả JWT riêng của hệ thống. Các API nghiệp vụ chỉ nhận JWT này, không nhận Google ID token.

Google yêu cầu backend dùng claim `sub` làm định danh ổn định, đồng thời kiểm tra chữ ký, `aud`, `iss` và `exp`. Code hiện còn yêu cầu `email_verified=true`. Xem [Google: Verify the Google ID token on your server side](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token).

## 1. Cấu hình Google

Trong Google Cloud Console:

1. Tạo/chọn project.
2. Cấu hình OAuth consent screen.
3. Tạo OAuth Client ID cho Web application.
4. Khai báo đúng JavaScript origins của frontend, ví dụ `http://localhost:3000`.
5. Đặt Client ID vào biến môi trường backend:

```properties
GOOGLE_CLIENT_ID=123456789-example.apps.googleusercontent.com
GOOGLE_ISSUER_URI=https://accounts.google.com
```

Frontend gửi **Google ID token** nằm trong trường `credential`, không gửi Google access token:

```http
POST /api/auth/login/customer/social
Content-Type: application/json

{
  "provider": "GOOGLE",
  "token": "<credential ID token từ Google Identity Services>"
}
```

`GOOGLE_CLIENT_ID` phải đúng audience của token. Backend tải public keys từ issuer Google và Spring Security cache key theo metadata của issuer.

## 2. Response thành công

Response đăng nhập Google thành công:

```json
{
  "token": "<JWT của hệ thống>",
  "tokenType": "Bearer",
  "expiresInMs": 3600000,
  "customer": {
    "id": "6fcf4a96-5d00-4f8f-bf64-d74ebdfaf392",
    "username": "google_...",
    "email": "customer@example.com",
    "fullName": "Nguyen Van A"
  }
}
```

Frontend lưu và gửi JWT hệ thống:

```http
Authorization: Bearer <JWT của hệ thống>
```

## 3. Quy tắc an toàn đang áp dụng

- Không dùng email làm định danh social; khóa thật là `(provider, provider_user_id)`.
- Không lưu Google ID token trong database.
- Không tự động gắn social vào customer đã có cùng email. API trả `409` để tránh chiếm tài khoản; sau này nên bổ sung endpoint liên kết social yêu cầu customer đăng nhập trước.
- Một customer có thể liên kết tối đa một tài khoản Google.
- Nếu customer bị `locked` hoặc không `active`, social login cũng bị từ chối.
- Production phải dùng HTTPS và giới hạn domain/origin tại Google Cloud Console.

## 4. Các API xác thực khác

### Nhân viên

- `POST /api/auth/login/employee`: đăng nhập nhân viên. `/api/auth/login` được giữ tương thích.
- `POST /api/auth/register/employee`: tạo nhân viên, nhiều role và nhiều phòng ban; yêu cầu authority `USER_CREATE`.
- `POST /api/auth/register/admin`: luồng admin cũ; yêu cầu `USER_CREATE_ADMIN`.

API tạo nhân viên không trả JWT của nhân viên mới, tránh người quản trị vô tình mạo danh. Nhân viên mới tự đăng nhập để nhận JWT.

### Khách hàng mật khẩu

- `POST /api/auth/register/customer`
- `POST /api/auth/login/customer`

### Đăng xuất

```http
POST /api/auth/logout
Authorization: Bearer <JWT của hệ thống>
```

JWT có `jti`; logout ghi `jti` vào `revoked_tokens` tới lúc token hết hạn. Client vẫn phải xóa token local. Có thể chạy job định kỳ xóa các dòng `expires_at < CURRENT_TIMESTAMP`.

## 5. Database

Chạy lần lượt V2, V3 rồi:

```powershell
psql -v ON_ERROR_STOP=1 -d commerce_db -f src/main/java/com/fashionsystem/fashion_system/db/migration/V4__social_auth_and_token_revocation.sql
```

V4 tạo `customer_social_accounts` và `revoked_tokens`. Với database đã tạo trước khi bỏ Facebook,
chạy thêm `V7__google_only_social_auth.sql` để từ chối mọi liên kết Facebook mới mà vẫn giữ dữ liệu cũ.
