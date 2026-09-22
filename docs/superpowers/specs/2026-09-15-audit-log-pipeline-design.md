# Audit Log Pipeline Design

## Goal

Khắc phục việc bắt log auth và log hệ thống không hoạt động đúng, loại việc ghi log khỏi request chính, dùng Kafka làm đường truyền bất đồng bộ và Redis làm cache/fallback, đồng thời đưa dữ liệu thật vào `/admin/logs`.

## Current Problems

- Trang `frontend/react-app/src/pages/admin/LoManagement.jsx` đang dùng dữ liệu seed tĩnh.
- Backend chỉ đọc `audit_logs` qua `/api/admin/audit-logs`; `auth_audit_logs` chưa được hợp nhất vào màn hình.
- `AuthAuditService` ghi JPA đồng bộ và chưa gán IP/User-Agent vào bản ghi.
- `AuditLogService.record()` ghi JPA trong transaction nghiệp vụ và có thể làm request thất bại khi không xác định được actor.
- Redis dependency và cache layer tồn tại nhưng cache mặc định tắt; rate limit vẫn dùng `ConcurrentHashMap` theo từng instance.
- Kafka chưa có dependency, topic, producer hoặc consumer.

## Approved Architecture

### 1. Unified event envelope

Mọi log mới đi qua một event envelope an toàn gồm `eventId`, `category` (`AUTH`, `BUSINESS`, `SYSTEM`), `action`, actor/user identifiers nếu có, entity identifiers nếu có, outcome, request metadata, sanitized metadata và `occurredAt`. Không đưa password, token, request body, password hash, OTP, raw email/phone hoặc credential vào event.

### 2. Non-blocking transport

`AuditEventPublisher` gửi event bằng Kafka producer với `CompletableFuture`; request không chờ broker acknowledgement và lỗi publish không được ném ngược vào request. Khi Kafka unavailable, publisher đẩy event vào Redis Stream có giới hạn cấu hình và TTL. Khi cả hai transport không dùng được, ứng dụng ghi warning có sampling/rate limit và tiếp tục xử lý nghiệp vụ.

Kafka topic, consumer group và Redis stream key cấu hình qua environment. Các integration này có kill switch; mặc định local vẫn phải khởi động được khi Kafka chưa chạy.

### 3. Persistence consumer

Kafka consumer (và Redis fallback consumer) chuẩn hóa event rồi lưu append-only vào bảng `system_audit_logs`. Bảng có index cho `created_at`, `category`, `action`, `actor_user_id`, `request_path` và không cho update/delete. Consumer phải idempotent theo `event_id` để Kafka retry không tạo bản ghi trùng.

Bảng cũ `audit_logs` và `auth_audit_logs` được giữ để tương thích dữ liệu hiện hữu. API log hợp nhất đọc cả dữ liệu mới và dữ liệu cũ theo cùng DTO; dữ liệu cũ được gắn category tương ứng. Log mới chỉ ghi qua pipeline mới.

### 4. System and auth capture

- Auth success, failed login, logout, refresh/revocation, lock và các auth event hiện có phát `AUTH` event.
- Một request filter bắt `SYSTEM` event cho API có ích, gồm method/path/status/duration/actor/IP/User-Agent; bỏ qua static resources, health endpoint và chính API đọc log.
- `BUSINESS` event thay thế việc gọi repository trực tiếp trong `AuditLogService`.
- Metadata chỉ gồm field được allowlist; không serialize request body.

### 5. Redis usage

- Rate limit dùng Redis atomic counter + expiry để hoạt động nhất quán giữa nhiều instance; nếu Redis lỗi thì fallback local best-effort hiện hữu và warning.
- API log có cache Redis ngắn hạn theo filter/page/sort; cache chỉ là read optimization, lỗi cache không làm API fail.
- Redis Stream là fallback transport, dùng consumer group và ack sau khi persistence thành công.

### 6. Admin logs API/UI

API `/api/admin/audit-logs` tiếp tục yêu cầu `LOG_VIEW`, hỗ trợ phân trang và filter category/action/entity/actor/time range. Response dùng một DTO thống nhất, có `level` suy ra từ category/outcome và `detail` an toàn cho UI.

`LoManagement.jsx` bỏ seed tĩnh, gọi API thật, hiển thị loading/error/empty state, phân trang theo server và refresh thủ công/tự động ở chu kỳ hợp lý. Bảng auth/system/business lấy cùng nguồn hợp nhất; không hiển thị dữ liệu giả khi backend chưa có bản ghi.

## Failure and Safety Rules

- Log pipeline là best-effort đối với request latency: producer/transport/persistence lỗi không được làm hỏng request nghiệp vụ.
- Persistence retry phải idempotent theo `event_id`; consumer chỉ ack sau khi commit thành công.
- Payload log được sanitize trước khi serialize; IP/User-Agent có giới hạn kích thước.
- Kafka và Redis credentials chỉ lấy từ environment; không commit secret.
- Khi integration bị tắt, hệ thống vẫn compile/start và API log vẫn đọc được dữ liệu cũ.

## Verification Criteria

- Test chứng minh publish không chặn request và không ném lỗi khi Kafka lỗi.
- Test chứng minh fallback Redis Stream và consumer idempotency.
- Test chứng minh auth event có IP/User-Agent và failed login không làm lộ credential.
- Test chứng minh request filter bắt status/duration nhưng loại trừ API đọc log.
- Test chứng minh API hợp nhất dữ liệu mới + dữ liệu cũ, filter và pagination.
- Frontend test chứng minh gọi API, render dữ liệu thật và các trạng thái loading/error/empty.
- Backend Maven test và frontend production build chạy thành công.
