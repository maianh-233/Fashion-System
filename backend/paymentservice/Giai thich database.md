# Payment Service Database Documentation

## Tổng quan

Database `payment_db` được sử dụng bởi `payment_service` để quản lý:

* Thanh toán đơn hàng
* Giao dịch với cổng thanh toán
* Hoàn tiền
* Webhook từ nhà cung cấp thanh toán

Các bảng chính:

| Table                  | Chức năng                                         |
| ---------------------- | ------------------------------------------------- |
| `payments`             | Lưu thông tin thanh toán của đơn hàng             |
| `payment_transactions` | Lưu các transaction phát sinh với payment gateway |
| `refunds`              | Quản lý hoàn tiền                                 |
| `payment_webhook_logs` | Log webhook từ cổng thanh toán                    |

---

# 1. Table: `payments`

## Chức năng

Bảng trung tâm quản lý toàn bộ thông tin thanh toán của một đơn hàng.

Mỗi bản ghi đại diện cho:

* Một lần thanh toán
* Một giao dịch thanh toán của order
* Trạng thái thanh toán hiện tại

Ví dụ:

* User thanh toán COD
* User thanh toán qua VNPay
* User thanh toán qua Momo
* User retry thanh toán

---

## Cấu trúc bảng

| Column             | Type          | Chức năng                       |
| ------------------ | ------------- | ------------------------------- |
| `id`               | UUID          | Khóa chính của payment          |
| `order_id`         | UUID          | ID đơn hàng từ `order_service`  |
| `payment_code`     | VARCHAR(50)   | Mã thanh toán duy nhất          |
| `method`           | VARCHAR(50)   | Phương thức thanh toán          |
| `amount`           | DECIMAL(14,2) | Số tiền thanh toán              |
| `status`           | VARCHAR(50)   | Trạng thái thanh toán           |
| `transaction_code` | VARCHAR(100)  | Mã transaction từ gateway       |
| `paid_at`          | TIMESTAMP     | Thời điểm thanh toán thành công |
| `created_at`       | TIMESTAMP     | Thời gian tạo payment           |
| `updated_at`       | TIMESTAMP     | Thời gian cập nhật gần nhất     |

---

## Giải thích chi tiết thuộc tính

### `id`

Khóa chính của payment.

Ví dụ:

```sql
550e8400-e29b-41d4-a716-446655440000
```

---

### `order_id`

Liên kết tới đơn hàng trong `order_service`.

Một order:

* Có thể có 1 payment
* Hoặc nhiều payment nếu retry thanh toán

---

### `payment_code`

Mã thanh toán duy nhất dùng để:

* Tra cứu
* Hiển thị cho user
* Mapping với hệ thống bên ngoài

Ví dụ:

```text
PAY-20260512-0001
```

---

### `method`

Phương thức thanh toán.

Ví dụ:

```text
COD
VNPAY
MOMO
PAYPAL
STRIPE
BANK_TRANSFER
```

---

### `amount`

Số tiền user cần thanh toán.

Ví dụ:

```text
1250000.00
```

---

### `status`

Trạng thái payment.

Ví dụ:

| Status       | Ý nghĩa               |
| ------------ | --------------------- |
| `PENDING`    | Chờ thanh toán        |
| `PROCESSING` | Đang xử lý            |
| `PAID`       | Thanh toán thành công |
| `FAILED`     | Thanh toán thất bại   |
| `CANCELLED`  | Đã hủy                |
| `REFUNDED`   | Đã hoàn tiền          |

---

### `transaction_code`

Mã transaction từ payment gateway.

Ví dụ:

```text
VNPAY_983746283
MOMO_TXN_123456
```

---

### `paid_at`

Thời điểm thanh toán thành công.

Nếu chưa thanh toán:

```sql
NULL
```

---

### `created_at`

Thời điểm tạo payment.

---

### `updated_at`

Thời điểm cập nhật payment gần nhất.

---

# 2. Table: `payment_transactions`

## Chức năng

Lưu lịch sử transaction giữa hệ thống và payment gateway.

Dùng để:

* Audit
* Retry
* Debug lỗi
* Theo dõi transaction lifecycle

Một payment có thể có nhiều transaction.

Ví dụ:

* Create payment
* Confirm payment
* Capture payment
* Refund payment

---

## Cấu trúc bảng

| Column                   | Type          | Chức năng                 |
| ------------------------ | ------------- | ------------------------- |
| `id`                     | UUID          | Khóa chính                |
| `payment_id`             | UUID          | Payment liên quan         |
| `gateway_transaction_id` | VARCHAR(100)  | Transaction ID từ gateway |
| `transaction_type`       | VARCHAR(30)   | Loại transaction          |
| `amount`                 | DECIMAL(14,2) | Số tiền transaction       |
| `status`                 | VARCHAR(50)   | Trạng thái transaction    |
| `raw_response`           | JSONB         | Response gốc từ gateway   |
| `created_at`             | TIMESTAMP     | Thời gian tạo transaction |

---

## Giải thích thuộc tính

### `payment_id`

Liên kết tới bảng `payments`.

---

### `gateway_transaction_id`

ID transaction phía gateway.

Ví dụ:

```text
txn_239847239847
```

---

### `transaction_type`

Loại transaction.

Ví dụ:

| Type        | Ý nghĩa             |
| ----------- | ------------------- |
| `AUTHORIZE` | Xác thực thanh toán |
| `CAPTURE`   | Trừ tiền            |
| `REFUND`    | Hoàn tiền           |
| `VOID`      | Hủy giao dịch       |

---

### `amount`

Số tiền transaction.

---

### `status`

Trạng thái transaction.

Ví dụ:

```text
SUCCESS
FAILED
PENDING
```

---

### `raw_response`

Lưu toàn bộ JSON response từ gateway.

Ví dụ:

```json
{
  "code": "00",
  "message": "Success",
  "bank": "VCB"
}
```

Mục đích:

* Debug
* Audit
* Đối soát

---

# 3. Table: `refunds`

## Chức năng

Quản lý yêu cầu hoàn tiền.

Cho phép:

* Refund toàn phần
* Refund một phần
* Theo dõi trạng thái hoàn tiền

---

## Cấu trúc bảng

| Column         | Type          | Chức năng                 |
| -------------- | ------------- | ------------------------- |
| `id`           | UUID          | Khóa chính                |
| `payment_id`   | UUID          | Payment được refund       |
| `refund_code`  | VARCHAR(50)   | Mã refund                 |
| `amount`       | DECIMAL(14,2) | Số tiền refund            |
| `reason`       | TEXT          | Lý do refund              |
| `status`       | VARCHAR(50)   | Trạng thái refund         |
| `requested_by` | UUID          | User/Admin yêu cầu refund |
| `requested_at` | TIMESTAMP     | Thời điểm yêu cầu         |
| `processed_at` | TIMESTAMP     | Thời điểm xử lý refund    |

---

## Giải thích thuộc tính

### `payment_id`

Payment được hoàn tiền.

---

### `refund_code`

Mã refund duy nhất.

Ví dụ:

```text
REF-20260512-001
```

---

### `amount`

Số tiền hoàn.

Có thể:

* Refund toàn phần
* Refund partial

---

### `reason`

Lý do hoàn tiền.

Ví dụ:

```text
Customer cancelled order
Product defect
Duplicate payment
```

---

### `status`

Trạng thái refund.

Ví dụ:

| Status       | Ý nghĩa              |
| ------------ | -------------------- |
| `PENDING`    | Chờ xử lý            |
| `PROCESSING` | Đang xử lý           |
| `COMPLETED`  | Hoàn tiền thành công |
| `FAILED`     | Hoàn tiền thất bại   |

---

### `requested_by`

ID user/admin yêu cầu refund.

---

### `requested_at`

Thời điểm tạo yêu cầu refund.

---

### `processed_at`

Thời điểm refund hoàn tất.

---

# 4. Table: `payment_webhook_logs`

## Chức năng

Lưu webhook gửi từ payment gateway.

Dùng để:

* Audit
* Retry xử lý webhook
* Debug
* Theo dõi callback

Ví dụ webhook:

* Thanh toán thành công
* Thanh toán thất bại
* Refund thành công

---

## Cấu trúc bảng

| Column         | Type         | Chức năng               |
| -------------- | ------------ | ----------------------- |
| `id`           | UUID         | Khóa chính              |
| `provider`     | VARCHAR(50)  | Nhà cung cấp thanh toán |
| `event_type`   | VARCHAR(100) | Loại sự kiện            |
| `payload`      | JSONB        | Nội dung webhook        |
| `processed`    | BOOLEAN      | Đã xử lý hay chưa       |
| `processed_at` | TIMESTAMP    | Thời điểm xử lý         |
| `created_at`   | TIMESTAMP    | Thời điểm nhận webhook  |

---

## Giải thích thuộc tính

### `provider`

Tên payment gateway.

Ví dụ:

```text
VNPAY
MOMO
STRIPE
PAYPAL
```

---

### `event_type`

Loại webhook event.

Ví dụ:

```text
PAYMENT_SUCCESS
PAYMENT_FAILED
REFUND_SUCCESS
```

---

### `payload`

Toàn bộ nội dung webhook JSON.

Ví dụ:

```json
{
  "amount": 100000,
  "transactionId": "abc123"
}
```

---

### `processed`

Đánh dấu webhook đã được xử lý hay chưa.

| Value   | Ý nghĩa    |
| ------- | ---------- |
| `true`  | Đã xử lý   |
| `false` | Chưa xử lý |

---

### `processed_at`

Thời điểm webhook được xử lý.

---

### `created_at`

Thời điểm hệ thống nhận webhook.

---

# 5. Các Index

## `idx_payments_order_id`

Tăng tốc truy vấn:

```sql
SELECT * FROM payments WHERE order_id = ?
```

---

## `idx_payment_transactions_payment_id`

Tăng tốc truy vấn transaction theo payment.

---

## `idx_refunds_payment_id`

Tăng tốc truy vấn refund theo payment.

---

## `idx_payment_webhook_logs_provider`

Tăng tốc truy vấn webhook theo provider.

---

# 6. Quan hệ giữa các bảng

```text
orders
   |
   | 1 - n
   v
payments
   |
   | 1 - n
   +------------------+
   |                  |
   v                  v
payment_transactions  refunds

payment_webhook_logs
(độc lập)
```

---

# 7. Flow hoạt động thực tế

## Thanh toán

```text
1. User tạo order
2. payment_service tạo payment
3. Redirect tới gateway
4. Gateway callback webhook
5. payment update status = PAID
6. order_service update order = CONFIRMED
```

---

## Refund

```text
1. Admin tạo refund request
2. payment_service gọi gateway refund API
3. Tạo payment_transaction loại REFUND
4. Update refunds.status
5. Nếu thành công:
   payment.status = REFUNDED
```
