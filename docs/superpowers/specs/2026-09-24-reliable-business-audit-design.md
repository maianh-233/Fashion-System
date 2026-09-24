# Reliable Business Audit Design

## Mục tiêu

Hệ thống phải tách biệt hoàn toàn hai loại nhật ký:

- `auth_audit_logs` chỉ ghi lịch sử xác thực: đăng nhập thành công, đăng nhập thất bại và đăng xuất.
- `audit_logs` ghi bằng chứng về mọi thay đổi dữ liệu do nhân viên hoặc tác vụ hệ thống thực hiện.

Một request nghiệp vụ thành công chỉ tạo một business audit event, kể cả khi request thay đổi nhiều bảng hoặc nhiều dòng. Audit phải cho biết ai thực hiện, thực hiện qua API nào, thay đổi gì, bao nhiêu dòng và chính xác những dòng nào. Audit không được làm transaction ghi `audit_logs` trở thành một phần của transaction nghiệp vụ và không được mất khi Kafka tạm ngừng.

## Phạm vi

Thiết kế áp dụng cho:

- Các HTTP API tạo, sửa, xóa, khôi phục, kích hoạt, vô hiệu hóa, phê duyệt, gán hoặc gỡ dữ liệu.
- Tác vụ nền/cron làm thay đổi dữ liệu, với actor `SYSTEM`.
- Thay đổi một dòng, nhiều dòng và nhiều bảng trong cùng một transaction nghiệp vụ.

Không tạo business audit cho request chỉ đọc hoặc transaction bị rollback. Các thay đổi kỹ thuật đối với bảng audit, outbox, session, token và dữ liệu vận hành nội bộ không được tự ghi ngược thành business audit.

## Nguyên tắc nhất quán và không mất dữ liệu

Không thể bảo đảm không mất event nếu chỉ gửi Kafka hoặc ghi file sau khi transaction nghiệp vụ đã commit: tiến trình có thể dừng trong khoảng giữa commit và thao tác đó. Vì vậy, transaction nghiệp vụ phải ghi thêm đúng một bản ghi delivery outbox trong cùng commit. Outbox là dấu giao nhận kỹ thuật, không phải audit chính và không được dùng làm API tra cứu lịch sử.

Luồng xử lý:

1. Request context xác định actor, request ID, HTTP method/path, IP và User-Agent. Tác vụ nền dùng actor `SYSTEM` cùng tên job.
2. Trong transaction nghiệp vụ, bộ thu thập change-set ghi nhận insert/update/delete của các entity nghiệp vụ.
3. Trước khi transaction hoàn tất, toàn bộ change-set được đóng gói thành một event và ghi một dòng vào audit outbox trong cùng transaction.
4. Nếu transaction rollback, cả dữ liệu nghiệp vụ và outbox đều không tồn tại.
5. Worker độc lập đọc outbox đã commit, append event vào file trong ngày và gửi event lên Kafka.
6. Kafka consumer mở transaction riêng để insert vào `audit_logs`.
7. Consumer xử lý idempotent theo `eventId`; Kafka retry hoặc job gửi bù không tạo audit trùng.

API không chờ Kafka consumer ghi `audit_logs`. Việc audit hỏng không rollback một nghiệp vụ đã commit. Outbox bảo đảm event đã commit luôn còn nguồn để worker thử lại.

## Mô hình event

Mỗi event có các trường cấp request:

- `eventId`: UUID duy nhất và là khóa idempotency.
- `occurredAt`: thời điểm transaction nghiệp vụ hoàn thành.
- `actorType`: `EMPLOYEE` hoặc `SYSTEM`.
- `actorUserId` và `username`: bắt buộc với nhân viên; null và `SYSTEM` với tác vụ nền.
- `requestId`, `method`, `path`, `ipAddress`, `userAgent`: thông tin truy vết HTTP; job nền dùng `jobName` thay cho thông tin HTTP không tồn tại.
- `action`: hành động nghiệp vụ chuẩn hóa, ví dụ `EMPLOYEE_UPDATE`, `PRODUCT_BULK_UPDATE`, `GOODS_RECEIPT_APPROVE`.
- `rowCount`: tổng số dòng nghiệp vụ bị thay đổi.
- `changes`: danh sách change item theo thứ tự ổn định.

Mỗi change item gồm:

- `table`: tên bảng vật lý hoặc tên resource chuẩn hóa.
- `rowId`: khóa chính của dòng, biểu diễn dưới dạng chuỗi để hỗ trợ cả UUID và khóa ghép.
- `operation`: `INSERT`, `UPDATE` hoặc `DELETE`.
- `changedFields`: danh sách field thực sự thay đổi.
- `oldValues`: snapshot các field trước thay đổi.
- `newValues`: snapshot các field sau thay đổi.

Đối với một dòng bị thay đổi nhiều lần trong cùng request, collector gộp thành một change item thể hiện trạng thái đầu tiên và trạng thái cuối cùng. Insert rồi delete trong cùng transaction không tạo thay đổi cuối cùng. `rowCount` là số change item sau khi gộp.

## Thu thập thay đổi

Một request-scoped hoặc transaction-scoped collector nhận change callbacks từ Hibernate cho insert, dirty update và delete. Collector phải loại trừ tối thiểu:

- `audit_logs`, `auth_audit_logs` và audit outbox.
- Refresh token, revoked token, OTP, session và các bảng hạ tầng tương tự.
- Các field kỹ thuật chỉ phục vụ optimistic locking nếu không mang ý nghĩa nghiệp vụ.

Các repository method dùng bulk JPQL/native SQL không đi qua entity callbacks. Mọi bulk mutation như vậy phải dùng một audit-aware adapter hoặc đăng ký trước danh sách row ID cùng old/new snapshot vào collector. Một test kiến trúc quét repository/service sẽ chặn bulk mutation mới không khai báo chiến lược audit.

Action nghiệp vụ không được suy đoán chỉ từ HTTP verb. Mutation endpoint hoặc application service phải khai báo action chuẩn hóa; infrastructure aspect mở và đóng audit context quanh transaction tương ứng. Các lời gọi service lồng nhau dùng chung context để vẫn chỉ sinh một event.

## Tách auth audit và business audit

`AuthAuditService` chỉ cho phép các action xác thực đã định nghĩa:

- `LOGIN_SUCCESS`
- `LOGIN_FAILED`
- `LOGOUT`

Các hằng và lời gọi như `EMPLOYEE_CREATED`, `EMPLOYEE_UPDATED`, `SUBORDINATE_ASSIGNED`, `SUBORDINATE_REMOVED` phải được loại khỏi `AuthAuditService` và chuyển sang business audit event tương ứng. Không hợp nhất dữ liệu hai bảng khi ghi.

API/UI có thể hiển thị hai nguồn ở hai màn hình hoặc hai chế độ riêng, nhưng không được làm mất ý nghĩa phân tách của bảng. Business audit API chỉ đọc `audit_logs`; auth audit API chỉ đọc `auth_audit_logs`.

## Outbox và worker giao nhận

Outbox lưu payload event bất biến cùng trạng thái giao nhận. Tối thiểu cần:

- `event_id` unique.
- `payload` JSONB.
- `occurred_at`, `created_at`.
- `file_appended_at`, `kafka_published_at`.
- `attempt_count`, `next_attempt_at`, `last_error`.

Worker khóa một batch bằng cơ chế tương đương `FOR UPDATE SKIP LOCKED` để nhiều instance không xử lý cùng dòng. Thứ tự giao nhận không phải điều kiện đúng vì `eventId` bảo đảm idempotency, nhưng event trong file nên được append theo `occurredAt`, rồi `eventId` khi có thể.

Worker chỉ đánh dấu `file_appended_at` sau khi append và flush thành công. Nếu tiến trình dừng giữa append và cập nhật trạng thái, lần thử lại có thể tạo dòng file trùng; reconciliation phải deduplicate theo `eventId`. Kafka publish được đánh dấu thành công từ callback acknowledgement, không phải ngay sau khi gọi `send()`.

Không xóa outbox chưa được xác nhận trong `audit_logs`. Bản ghi đã hoàn tất có thể được dọn theo retention riêng sau khi file ngày tương ứng đã đối soát và xóa thành công.

## File log theo ngày

File spool dùng JSON Lines, một event trên một dòng, chia theo ngày địa phương `Asia/Saigon`. Thư mục và tên file cấu hình được; tên mặc định theo mẫu `business-audit-YYYY-MM-DD.jsonl`.

Mỗi dòng chứa payload chuẩn hóa và:

- `payloadHash`: SHA-256 của payload canonical.
- `previousHash`: hash của dòng hợp lệ liền trước trong file.
- `lineHash`: SHA-256 của `previousHash + payloadHash + eventId`.

Hash chain phát hiện sửa/xóa/chèn nội dung trong file nhưng không thay thế quyền filesystem. Thư mục phải chỉ cho service account ghi và operator được đọc; ứng dụng không cung cấp API sửa file. Secret, password hash, token, OTP và credential không bao giờ xuất hiện trong payload hoặc file.

Writer phải dùng file lock nội bộ tiến trình và append an toàn. Nếu nhiều application instance cùng chạy, mỗi instance dùng file riêng có hậu tố instance ID hoặc dùng một spool writer chuyên trách; không cho nhiều host ghi chung một file không có distributed lock.

## Kafka và consumer

Kafka topic business audit tách khỏi auth audit. Producer dùng `eventId` làm message key. Producer không chặn request nghiệp vụ; worker chịu retry với exponential backoff và giữ outbox khi broker không sẵn sàng.

Consumer:

- Validate schema/version và bắt buộc `eventId`.
- Insert `audit_logs` trong transaction `REQUIRES_NEW` hoặc transaction consumer tương đương.
- Dùng unique constraint trên `event_id`; duplicate được coi là thành công.
- Chỉ acknowledge Kafka sau khi transaction DB commit.
- Event không hợp lệ được đưa vào dead-letter topic và giữ outbox/file để operator đối soát, không âm thầm bỏ qua.

## Lưu trữ `audit_logs`

`audit_logs` giữ một dòng cho mỗi request/job event, không phải một dòng cho mỗi entity. Schema cần hỗ trợ trực tiếp:

- Actor và request metadata.
- `action`.
- `row_count`.
- `changes` JSONB.
- `event_id` unique.
- `created_at`/`occurred_at`.

Bảng là append-only. Database trigger từ chối `UPDATE` và `DELETE` đối với role ứng dụng. Quyền xem dùng permission `LOG_VIEW`; không có API sửa/xóa audit. Query hỗ trợ thời gian, actor, action, table/resource, row ID và request ID.

## Đối soát lúc 0 giờ

Scheduler chạy sau 00:00 theo `Asia/Saigon` cho file của ngày vừa kết thúc:

1. Đọc file theo streaming, kiểm tra JSON, hash chain và deduplicate `eventId`.
2. Query `audit_logs` theo batch `eventId`, không query từng dòng.
3. Với event thiếu, publish lại Kafka và tiếp tục giữ file.
4. Chạy lại kiểm tra sau acknowledgement/chu kỳ retry; không giả định publish thành công đồng nghĩa DB đã ghi.
5. Chỉ xóa file khi mọi event hợp lệ đã tồn tại trong `audit_logs`, không có hash lỗi và không còn event outbox liên quan chưa hoàn tất.

Nếu file lỗi hash, job phải giữ nguyên file, ghi cảnh báo mức cao và không tự sửa hoặc xóa. Nếu Kafka/DB lỗi, job giữ file và retry ở lần chạy tiếp theo; operator có thể kích hoạt reconciliation thủ công theo ngày bằng command nội bộ có kiểm soát.

## Xử lý lỗi và quan sát vận hành

Các metric/cảnh báo tối thiểu:

- Số outbox pending và tuổi event pending lâu nhất.
- Số lần Kafka publish lỗi.
- Consumer lag và dead-letter count.
- Số event thiếu khi đối soát.
- Lỗi hash/file và số file quá hạn chưa xóa.

Không log toàn bộ payload audit vào application log vì có thể lộ dữ liệu cá nhân. Error log chỉ ghi `eventId`, trạng thái và loại lỗi đã rút gọn.

## Bảo mật và dữ liệu nhạy cảm

Sanitizer hoạt động trước khi payload vào outbox. Danh sách deny mặc định gồm password, password hash, token, refresh token, authorization header, cookie, OTP, secret, API key và nội dung file nhị phân. Các field nhạy cảm được thay bằng marker `[REDACTED]`; dữ liệu không cần thiết cho bằng chứng không được thu thập.

Actor lấy từ security context phía server, không tin username/user ID do client gửi. IP xử lý `X-Forwarded-For` chỉ khi ứng dụng chạy sau trusted proxy; nếu không dùng remote address thực tế.

## Chuyển đổi dữ liệu và tương thích

Dữ liệu lịch sử trong `auth_audit_logs` đã ghi sai loại không được âm thầm xóa. Migration một lần sẽ:

- Sao chép các action nghiệp vụ cũ sang `audit_logs` với cờ metadata `migratedFromAuthAudit=true` khi đủ thông tin.
- Giữ nguyên nguồn cũ để bảo toàn dấu vết, hoặc chỉ xóa sau khi có quyết định retention riêng được phê duyệt.
- Ngăn code mới ghi action nghiệp vụ vào `auth_audit_logs`.

Các business audit hiện hữu tiếp tục đọc được. API DTO được mở rộng tương thích để trả `rowCount` và `changes`; client cũ không phụ thuộc các field mới vẫn hoạt động.

## Kiểm thử chấp nhận

Tối thiểu phải chứng minh:

1. Login/logout chỉ ghi `auth_audit_logs`, không ghi business audit.
2. Một request sửa một dòng tạo một event với đúng actor, before/after và field thay đổi.
3. Một request sửa nhiều bảng/dòng tạo đúng một event với `rowCount` và danh sách đầy đủ.
4. Transaction rollback không tạo outbox, file hay `audit_logs`.
5. Kafka ngừng hoạt động không làm request nghiệp vụ thất bại; outbox và file giữ event để gửi lại.
6. Dừng tiến trình tại các điểm giữa outbox, file, Kafka và consumer không làm mất event; retry không tạo audit DB trùng.
7. Job 0 giờ gửi bù event thiếu và không xóa file khi DB chưa đủ.
8. Job chỉ xóa file sau khi toàn bộ `eventId` đã có trong DB và hash chain hợp lệ.
9. Bulk update/delete được gom một event và liệt kê đúng toàn bộ row ID.
10. Password, token, OTP và secret không xuất hiện trong outbox, Kafka payload, file hoặc `audit_logs`.
11. Actor `SYSTEM` và job name được ghi đúng cho tác vụ nền.
12. `audit_logs` từ chối update/delete và API không cung cấp mutation endpoint cho audit.

## Ngoài phạm vi

- Dùng audit log làm cơ chế rollback dữ liệu nghiệp vụ.
- Ghi toàn bộ request/response body không chọn lọc.
- Cam kết chống sửa bởi quản trị viên có toàn quyền trên cả database, filesystem và khóa hạ tầng. Muốn đạt mức đó cần kho lưu trữ WORM hoặc dịch vụ ký/neo hash độc lập.
