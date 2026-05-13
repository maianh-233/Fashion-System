# Notification Service Database Documentation

## Tổng quan

Database `notification_db` được sử dụng cho `notification_service` trong hệ thống fashion e-commerce/microservice.
Service này chịu trách nhiệm:

* Gửi thông báo cho người dùng
* Quản lý lịch sử gửi thông báo
* Quản lý template thông báo
* Quản lý cấu hình nhận thông báo của user
* Hỗ trợ nhiều kênh gửi:

  * EMAIL
  * SMS
  * PUSH_NOTIFICATION
  * IN_APP

---

# 1. Table: `notifications`

## Chức năng

Bảng `notifications` lưu toàn bộ thông báo được tạo trong hệ thống.

Đây là bảng trung tâm của notification service.

Ví dụ:

* Thông báo đặt hàng thành công
* Thông báo khuyến mãi
* Thông báo giao hàng
* OTP email
* Thông báo hoàn tiền

---

## Cấu trúc thuộc tính

| Column           | Kiểu dữ liệu | Ý nghĩa                                                    |
| ---------------- | ------------ | ---------------------------------------------------------- |
| `id`             | UUID         | ID duy nhất của notification                               |
| `recipient_id`   | UUID         | ID người nhận thông báo                                    |
| `recipient_type` | VARCHAR(20)  | Loại người nhận (`USER`, `ADMIN`, `STAFF`)                 |
| `channel`        | VARCHAR(20)  | Kênh gửi (`EMAIL`, `SMS`, `PUSH`, `IN_APP`)                |
| `title`          | VARCHAR(255) | Tiêu đề thông báo                                          |
| `content`        | TEXT         | Nội dung thông báo                                         |
| `reference_type` | VARCHAR(50)  | Loại đối tượng liên quan (`ORDER`, `PAYMENT`, `PROMOTION`) |
| `reference_id`   | UUID         | ID của đối tượng liên quan                                 |
| `status`         | VARCHAR(20)  | Trạng thái gửi (`PENDING`, `SENT`, `FAILED`, `CANCELLED`)  |
| `scheduled_at`   | TIMESTAMP    | Thời gian dự kiến gửi                                      |
| `created_at`     | TIMESTAMP    | Thời gian tạo notification                                 |
| `sent_at`        | TIMESTAMP    | Thời gian gửi thành công                                   |

---

## Ý nghĩa nghiệp vụ

### `recipient_id`

Xác định người nhận notification.

Ví dụ:

* User mua hàng
* Admin nhận cảnh báo hệ thống
* Staff nhận thông báo đơn hàng

---

### `channel`

Cho phép một notification được gửi qua nhiều phương thức khác nhau.

Ví dụ:

| Channel | Ý nghĩa                  |
| ------- | ------------------------ |
| EMAIL   | Gửi email                |
| SMS     | Gửi tin nhắn             |
| PUSH    | Push notification        |
| IN_APP  | Thông báo trong ứng dụng |

---

### `reference_type` + `reference_id`

Dùng để liên kết notification với entity khác trong hệ thống.

Ví dụ:

| reference_type | reference_id |
| -------------- | ------------ |
| ORDER          | order_id     |
| PAYMENT        | payment_id   |
| PROMOTION      | promotion_id |

Điều này giúp frontend mở đúng màn hình liên quan khi user click notification.

---

### `status`

Theo dõi trạng thái gửi.

Ví dụ:

| Status    | Ý nghĩa           |
| --------- | ----------------- |
| PENDING   | Chờ gửi           |
| SENT      | Đã gửi thành công |
| FAILED    | Gửi thất bại      |
| CANCELLED | Đã hủy            |

---

## Index

### `idx_notifications_recipient_id`

```sql
CREATE INDEX idx_notifications_recipient_id 
ON notifications(recipient_id);
```

Tăng tốc truy vấn:

```sql
SELECT * FROM notifications 
WHERE recipient_id = ?;
```

---

### `idx_notifications_reference`

```sql
CREATE INDEX idx_notifications_reference 
ON notifications(reference_type, reference_id);
```

Tăng tốc truy vấn notification theo entity liên quan.

Ví dụ:

```sql
SELECT * FROM notifications
WHERE reference_type = 'ORDER'
AND reference_id = ?;
```

---

# 2. Table: `notification_logs`

## Chức năng

Bảng `notification_logs` lưu lịch sử gửi notification thực tế.

Dùng để:

* Audit
* Debug lỗi gửi mail/SMS
* Theo dõi retry
* Theo dõi provider

Một notification có thể có nhiều log.

Ví dụ:

* Lần 1 gửi thất bại
* Retry lần 2 thành công

---

## Cấu trúc thuộc tính

| Column            | Kiểu dữ liệu | Ý nghĩa                                                 |
| ----------------- | ------------ | ------------------------------------------------------- |
| `id`              | UUID         | ID log                                                  |
| `notification_id` | UUID         | Notification liên quan                                  |
| `channel`         | VARCHAR(20)  | Kênh gửi                                                |
| `destination`     | VARCHAR(255) | Địa chỉ nhận (`email`, `phone`)                         |
| `status`          | VARCHAR(20)  | Trạng thái gửi                                          |
| `error_message`   | TEXT         | Nội dung lỗi nếu gửi thất bại                           |
| `provider`        | VARCHAR(50)  | Nhà cung cấp dịch vụ (`Firebase`, `SendGrid`, `Twilio`) |
| `sent_at`         | TIMESTAMP    | Thời gian gửi                                           |

---

## Ý nghĩa nghiệp vụ

### `destination`

Lưu địa chỉ gửi thực tế.

Ví dụ:

* Email: `abc@gmail.com`
* Phone: `098xxxx`
* Device token

---

### `provider`

Cho phép hệ thống hỗ trợ nhiều bên thứ ba.

Ví dụ:

| Provider | Chức năng         |
| -------- | ----------------- |
| SendGrid | Gửi email         |
| Twilio   | SMS               |
| Firebase | Push notification |

---

### `error_message`

Lưu lỗi chi tiết khi gửi thất bại.

Ví dụ:

```text
SMTP Authentication Failed
```

hoặc:

```text
Invalid phone number
```

---

## Index

### `idx_notification_logs_notification_id`

```sql
CREATE INDEX idx_notification_logs_notification_id
ON notification_logs(notification_id);
```

Tăng tốc truy vấn log theo notification.

---

# 3. Table: `notification_templates`

## Chức năng

Bảng `notification_templates` lưu template dùng để tạo notification động.

Giúp:

* Tái sử dụng nội dung
* Dễ chỉnh sửa
* Hỗ trợ đa ngôn ngữ
* Tách logic khỏi source code

---

## Cấu trúc thuộc tính

| Column             | Kiểu dữ liệu | Ý nghĩa              |
| ------------------ | ------------ | -------------------- |
| `id`               | UUID         | ID template          |
| `code`             | VARCHAR(100) | Mã template duy nhất |
| `title_template`   | VARCHAR(255) | Template tiêu đề     |
| `content_template` | TEXT         | Template nội dung    |
| `channel`          | VARCHAR(20)  | Kênh sử dụng         |
| `active`           | BOOLEAN      | Trạng thái hoạt động |
| `created_at`       | TIMESTAMP    | Thời gian tạo        |
| `updated_at`       | TIMESTAMP    | Thời gian cập nhật   |

---

## Ví dụ template

### ORDER_SUCCESS

```text
Xin chào {{customer_name}},
Đơn hàng {{order_code}} đã được xác nhận.
```

---

## Ý nghĩa nghiệp vụ

### `code`

Dùng để truy xuất template trong code.

Ví dụ:

```java
ORDER_SUCCESS
RESET_PASSWORD
PAYMENT_SUCCESS
```

---

### `active`

Cho phép bật/tắt template mà không cần xóa dữ liệu.

---

# 4. Table: `user_notification_preferences`

## Chức năng

Bảng này lưu cấu hình nhận notification của user.

Cho phép user:

* Bật/tắt email
* Tắt SMS
* Chỉ nhận push notification

---

## Cấu trúc thuộc tính

| Column       | Kiểu dữ liệu | Ý nghĩa                    |
| ------------ | ------------ | -------------------------- |
| `id`         | UUID         | ID preference              |
| `user_id`    | UUID         | ID user                    |
| `channel`    | VARCHAR(20)  | Kênh notification          |
| `enabled`    | BOOLEAN      | Có cho phép nhận hay không |
| `created_at` | TIMESTAMP    | Thời gian tạo              |
| `updated_at` | TIMESTAMP    | Thời gian cập nhật         |

---

## Ý nghĩa nghiệp vụ

### `enabled`

| Giá trị | Ý nghĩa               |
| ------- | --------------------- |
| TRUE    | User cho phép nhận    |
| FALSE   | User tắt notification |

---

## UNIQUE Constraint

```sql
UNIQUE (user_id, channel)
```

Đảm bảo:

* Một user chỉ có một cấu hình cho mỗi channel

Ví dụ:

| user_id | channel |
| ------- | ------- |
| A       | EMAIL   |

Không thể tồn tại thêm:

| user_id | channel |
| ------- | ------- |
| A       | EMAIL   |

---

## Index

### `idx_user_notification_preferences_user_id`

```sql
CREATE INDEX idx_user_notification_preferences_user_id
ON user_notification_preferences(user_id);
```

Tăng tốc truy vấn preference theo user.

---

# Luồng hoạt động tổng quát

## 1. Tạo notification

Service khác gọi:

```text
Order Service
→ Notification Service
```

Tạo record trong:

```text
notifications
```

---

## 2. Kiểm tra preference user

Kiểm tra:

```text
user_notification_preferences
```

Nếu user tắt EMAIL → không gửi mail.

---

## 3. Lấy template

Truy vấn:

```text
notification_templates
```

Sinh nội dung thực tế.

---

## 4. Gửi notification

Thông qua provider:

* SendGrid
* Firebase
* Twilio

---

## 5. Ghi log

Kết quả được lưu vào:

```text
notification_logs
```

---

# Quan hệ giữa các bảng

```text
notifications
    |
    |--< notification_logs

users
    |
    |--< user_notification_preferences
```

---

# Kiến trúc nghiệp vụ đề xuất

## Notification Service nên hỗ trợ

### Async Processing

Khuyến nghị dùng:

* Kafka
* RabbitMQ

Để xử lý gửi notification bất đồng bộ.

---

## Retry Mechanism

Khi gửi thất bại:

* Retry 3 lần
* Exponential Backoff

---

## Template Engine

Khuyến nghị:

* Thymeleaf
* FreeMarker
* Mustache

---

## Multi-channel Strategy

Nên thiết kế:

```text
NotificationSender Interface
```

Ví dụ:

```text
EmailSender
SmsSender
PushSender
```

