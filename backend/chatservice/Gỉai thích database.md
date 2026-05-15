# Chat Service Database Documentation

## Tổng quan

Database `chat_db` phục vụ cho `chat_service`, hỗ trợ:

* Chat giữa khách hàng và nhân viên liên quan đến đơn hàng
* Chat nội bộ giữa nhân viên/admin
* Quản lý trạng thái tin nhắn
* Đính kèm file trong chat
* Phân loại vấn đề/phản hồi của khách hàng

---

# 1. Bảng `order_chat_rooms`

Lưu thông tin phòng chat liên quan đến một đơn hàng.

## Mục đích

* Mỗi đơn hàng có một phòng chat riêng
* Hỗ trợ customer trao đổi với staff
* Theo dõi trạng thái xử lý của cuộc hội thoại

---

## Cấu trúc bảng

| Thuộc tính          | Kiểu dữ liệu | Ý nghĩa                      |
| ------------------- | ------------ | ---------------------------- |
| `id`                | UUID         | ID duy nhất của phòng chat   |
| `order_id`          | UUID         | ID đơn hàng liên kết         |
| `customer_id`       | UUID         | ID khách hàng                |
| `assigned_staff_id` | UUID         | Nhân viên phụ trách chat     |
| `status`            | VARCHAR(50)  | Trạng thái phòng chat        |
| `last_message_at`   | TIMESTAMP    | Thời gian tin nhắn cuối cùng |
| `created_at`        | TIMESTAMP    | Thời gian tạo phòng chat     |
| `closed_at`         | TIMESTAMP    | Thời gian đóng phòng chat    |

---

## Giá trị status gợi ý

| Giá trị    | Ý nghĩa        |
| ---------- | -------------- |
| `OPEN`     | Đang hoạt động |
| `PENDING`  | Chờ phản hồi   |
| `RESOLVED` | Đã xử lý       |
| `CLOSED`   | Đã đóng        |

---

# 2. Bảng `order_chat_messages`

Lưu toàn bộ tin nhắn trong phòng chat đơn hàng.

## Mục đích

* Lưu lịch sử chat
* Hỗ trợ nhiều loại tin nhắn
* Có thể mở rộng cho automation/chatbot

---

## Cấu trúc bảng

| Thuộc tính       | Kiểu dữ liệu | Ý nghĩa                  |
| ---------------- | ------------ | ------------------------ |
| `id`             | UUID         | ID tin nhắn              |
| `room_id`        | UUID         | Phòng chat chứa tin nhắn |
| `sender_id`      | UUID         | Người gửi                |
| `sender_type`    | VARCHAR(20)  | Loại người gửi           |
| `message_type`   | VARCHAR(20)  | Loại tin nhắn            |
| `content`        | TEXT         | Nội dung tin nhắn        |
| `related_action` | VARCHAR(50)  | Action liên quan         |
| `metadata`       | JSONB        | Dữ liệu mở rộng          |
| `created_at`     | TIMESTAMP    | Thời gian gửi            |

---

## sender_type gợi ý

| Giá trị    | Ý nghĩa    |
| ---------- | ---------- |
| `CUSTOMER` | Khách hàng |
| `STAFF`    | Nhân viên  |
| `SYSTEM`   | Hệ thống   |
| `BOT`      | Chatbot/AI |

---

## message_type gợi ý

| Giá trị  | Ý nghĩa       |
| -------- | ------------- |
| `TEXT`   | Văn bản       |
| `IMAGE`  | Hình ảnh      |
| `FILE`   | File          |
| `SYSTEM` | Tin hệ thống  |
| `ACTION` | Tin hành động |

---

## metadata ví dụ

```json
{
  "tracking_code": "VN123456",
  "refund_amount": 150000
}
```

---

# 3. Bảng `order_chat_message_status`

Lưu trạng thái đọc/xử lý của từng user đối với từng tin nhắn.

## Mục đích

* Hỗ trợ seen/read message
* Hỗ trợ delivered/read status
* Tracking trạng thái message theo user

---

## Cấu trúc bảng

| Thuộc tính   | Kiểu dữ liệu | Ý nghĩa            |
| ------------ | ------------ | ------------------ |
| `message_id` | UUID         | Tin nhắn           |
| `user_id`    | UUID         | User nhận tin      |
| `status`     | VARCHAR(20)  | Trạng thái         |
| `updated_at` | TIMESTAMP    | Thời gian cập nhật |

---

## Giá trị status gợi ý

| Giá trị     | Ý nghĩa |
| ----------- | ------- |
| `SENT`      | Đã gửi  |
| `DELIVERED` | Đã nhận |
| `READ`      | Đã xem  |

---

## Primary Key

```sql
PRIMARY KEY (message_id, user_id)
```

Đảm bảo:

* Một user chỉ có một trạng thái cho một tin nhắn.

---

# 4. Bảng `order_issue_types`

Danh mục loại vấn đề của đơn hàng.

## Mục đích

* Chuẩn hóa các loại khiếu nại/vấn đề
* Hỗ trợ thống kê và filtering

---

## Cấu trúc bảng

| Thuộc tính | Kiểu dữ liệu | Ý nghĩa        |
| ---------- | ------------ | -------------- |
| `code`     | VARCHAR(50)  | Mã loại vấn đề |
| `name`     | VARCHAR(255) | Tên hiển thị   |

---

## Ví dụ dữ liệu

| code             | name              |
| ---------------- | ----------------- |
| `LATE_DELIVERY`  | Giao hàng chậm    |
| `WRONG_PRODUCT`  | Sai sản phẩm      |
| `REFUND_REQUEST` | Yêu cầu hoàn tiền |

---

# 5. Bảng `internal_chat_rooms`

Lưu phòng chat nội bộ giữa nhân viên/admin.

## Mục đích

* Chat nội bộ
* Hỗ trợ teamwork
* Hỗ trợ xử lý đơn hàng/phản hồi

---

## Cấu trúc bảng

| Thuộc tính   | Kiểu dữ liệu | Ý nghĩa        |
| ------------ | ------------ | -------------- |
| `id`         | UUID         | ID phòng chat  |
| `created_by` | UUID         | Người tạo      |
| `room_type`  | VARCHAR(20)  | Loại phòng     |
| `status`     | VARCHAR(20)  | Trạng thái     |
| `created_at` | TIMESTAMP    | Thời gian tạo  |
| `closed_at`  | TIMESTAMP    | Thời gian đóng |

---

## room_type gợi ý

| Giá trị   | Ý nghĩa      |
| --------- | ------------ |
| `PRIVATE` | Chat riêng   |
| `GROUP`   | Chat nhóm    |
| `SUPPORT` | Hỗ trợ xử lý |

---

# 6. Bảng `internal_chat_messages`

Lưu tin nhắn trong chat nội bộ.

## Mục đích

* Trao đổi nội bộ
* Có thể tích hợp AI intent analysis

---

## Cấu trúc bảng

| Thuộc tính    | Kiểu dữ liệu | Ý nghĩa         |
| ------------- | ------------ | --------------- |
| `id`          | UUID         | ID tin nhắn     |
| `room_id`     | UUID         | Phòng chat      |
| `sender_id`   | UUID         | Người gửi       |
| `sender_type` | VARCHAR(20)  | Loại người gửi  |
| `content`     | TEXT         | Nội dung        |
| `intent`      | VARCHAR(100) | Ý định message  |
| `metadata`    | JSONB        | Dữ liệu mở rộng |
| `created_at`  | TIMESTAMP    | Thời gian gửi   |

---

## intent gợi ý

| Giá trị           | Ý nghĩa          |
| ----------------- | ---------------- |
| `ESCALATION`      | Chuyển cấp xử lý |
| `ORDER_CHECK`     | Kiểm tra đơn     |
| `REFUND_APPROVAL` | Duyệt hoàn tiền  |

---

# 7. Bảng `chat_attachments`

Lưu file đính kèm trong chat.

## Mục đích

* Upload file/hình ảnh
* Liên kết attachment với message

---

## Cấu trúc bảng

| Thuộc tính   | Kiểu dữ liệu | Ý nghĩa            |
| ------------ | ------------ | ------------------ |
| `id`         | UUID         | ID file            |
| `message_id` | UUID         | Tin nhắn chứa file |
| `file_name`  | VARCHAR(255) | Tên file           |
| `file_url`   | TEXT         | URL file           |
| `file_type`  | VARCHAR(100) | MIME type          |
| `file_size`  | BIGINT       | Kích thước file    |
| `created_at` | TIMESTAMP    | Thời gian upload   |

---

## Ví dụ file_type

| Giá trị           | Ý nghĩa  |
| ----------------- | -------- |
| `image/png`       | Ảnh PNG  |
| `image/jpeg`      | Ảnh JPG  |
| `application/pdf` | File PDF |

---

# 8. Các Index

## `idx_order_chat_messages_room_id`

```sql
CREATE INDEX idx_order_chat_messages_room_id 
ON order_chat_messages(room_id);
```

### Mục đích

* Tăng tốc truy vấn lấy message theo room chat.

---

## `idx_order_chat_room_order_id`

```sql
CREATE INDEX idx_order_chat_room_order_id 
ON order_chat_rooms(order_id);
```

### Mục đích

* Tăng tốc tìm phòng chat theo order.

---

## `idx_internal_chat_messages_room_id`

```sql
CREATE INDEX idx_internal_chat_messages_room_id 
ON internal_chat_messages(room_id);
```

### Mục đích

* Tăng tốc truy vấn message nội bộ.

---

## `idx_chat_attachments_message_id`

```sql
CREATE INDEX idx_chat_attachments_message_id 
ON chat_attachments(message_id);
```

### Mục đích

* Tăng tốc lấy attachment theo message.

---

# Luồng hoạt động tổng quát

## Chat đơn hàng

```text
Customer -> order_chat_room -> order_chat_messages
                                 |
                                 -> chat_attachments
                                 |
                                 -> order_chat_message_status
```

---

## Chat nội bộ

```text
Staff/Admin -> internal_chat_rooms -> internal_chat_messages
```

---

