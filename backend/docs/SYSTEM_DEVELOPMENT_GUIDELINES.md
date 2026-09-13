# System Development Guidelines

> Đây là tài liệu quy định chung khi phát triển và chỉnh sửa hệ thống.
>
> **BẮT BUỘC đọc tài liệu này trước khi tạo, sửa, xóa hoặc refactor code.**

---

# 1. Core Principles

Mọi thay đổi trong hệ thống phải tuân thủ các nguyên tắc:

```text
Understand before coding
Reuse before creating
Minimal change
Single responsibility
Keep architecture consistent
Secure by default
Test before completion
```

Không được viết code chỉ để "làm cho chạy".

Code phải phù hợp với kiến trúc, convention và cách tổ chức hiện tại của project.

---

# 2. MUST READ CODEBASE BEFORE CODING

Trước khi tạo bất kỳ:

* class
* interface
* method
* utility
* helper
* service
* controller
* DTO
* repository
* component

phải đọc và phân tích codebase hiện tại.

Tối thiểu phải kiểm tra:

```text
Project structure
Existing classes
Existing interfaces
Existing services
Existing helpers
Existing utilities
Existing DTOs
Existing exception handling
Existing configurations
Existing dependencies
Existing tests
```

Nếu yêu cầu liên quan đến một module cụ thể, phải đọc module đó trước.

---

# 3. REUSE BEFORE CREATE

Đây là nguyên tắc bắt buộc.

Trước khi viết một function/method/class mới, phải kiểm tra:

> "Project đã có code nào thực hiện chức năng tương tự chưa?"

Nếu đã có:

**PHẢI TÁI SỬ DỤNG.**

Không được tạo implementation thứ hai chỉ vì:

* tên method khác
* muốn code ngắn hơn
* muốn viết theo style cá nhân
* không muốn tìm code cũ
* implementation hiện tại hơi khác

Ví dụ không được tạo:

```java
DateHelper2
StringUtilsCustom
NewValidationUtil
CommonHelper
BaseUtility
```

nếu project đã có implementation tương đương.

---

# 4. DO NOT DUPLICATE LOGIC

Không copy/paste cùng một business logic ở nhiều nơi.

Nếu phát hiện logic đã tồn tại:

```text
A Service
B Service
C Controller
```

đang lặp lại cùng một logic, phải đánh giá khả năng tái sử dụng abstraction hiện tại.

Tuy nhiên:

**Không tự ý refactor lớn chỉ vì phát hiện duplicate.**

Nếu refactor nằm ngoài scope của task, phải giữ nguyên và báo cáo đề xuất riêng.

---

# 5. COMMENTS AND DOCUMENTATION

Khi viết **class hoặc method**, phải có phần chú thích phù hợp.

## Class

Các class quan trọng phải có JavaDoc:

```java
/**
 * Provides common operations for ...
 *
 * <p>This class is responsible for ...</p>
 */
public class ExampleService {
}
```

## Method

Các public method nên có JavaDoc mô tả:

* mục đích
* input
* output
* exception nếu cần
* behavior quan trọng

Ví dụ:

```java
/**
 * Uploads a file to the configured storage provider.
 *
 * @param file file to upload
 * @param folder destination folder
 * @return upload result containing public URL and metadata
 * @throws StorageException when the upload fails
 */
public StorageUploadResult upload(
        MultipartFile file,
        String folder
) {
}
```

## Không comment vô nghĩa

Không viết:

```java
// set name
user.setName(name);

// return user
return user;
```

Comment phải giải thích **why**, không chỉ nhắc lại **what**.

---

# 6. CODE NAMING

Tên phải thể hiện đúng trách nhiệm.

Ưu tiên:

```text
ProductService
OrderService
StorageService
MailService
PdfService
ExcelService
```

Thay vì:

```text
Manager
Helper2
Util
Common
Processor
Handler
```

nếu tên đó không thể hiện rõ responsibility.

Tên method phải mô tả hành động:

```text
createOrder()
updateProduct()
calculateTotal()
uploadFile()
generateInvoice()
sendMail()
```

Không dùng tên mơ hồ:

```text
process()
handle()
doSomething()
execute()
```

trừ khi context thực sự rõ ràng.

---

# 7. RESPONSIBILITY

Mỗi layer phải có trách nhiệm rõ ràng.

Ví dụ backend:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Controller:

* nhận request
* validate request cơ bản
* gọi service
* trả response

Service:

* business logic
* transaction
* orchestration

Repository:

* database access

Entity:

* database/domain representation

DTO:

* API/input/output representation

Mapper:

* Entity ↔ DTO conversion

Infrastructure:

* external systems
* email
* storage
* PDF
* Excel
* external APIs

Không đưa business logic vào Controller.

Không đưa database query vào Controller.

Không đưa SMTP logic vào OrderService.

Không đưa Cloudinary logic vào ProductController.

---

# 8. INFRASTRUCTURE SERVICES

Infrastructure service phải được thiết kế để tái sử dụng.

Ví dụ:

```text
StorageService
MailService
PdfService
ExcelService
```

Business module chỉ sử dụng chúng.

Ví dụ:

```text
OrderService
    ↓
PdfService
    ↓
MailService
```

Không tạo implementation riêng cho từng business module nếu không cần thiết.

---

# 9. DO NOT MODIFY OUTSIDE SCOPE

Khi được giao một task, chỉ sửa những phần cần thiết để hoàn thành task.

Không tự ý:

* sửa Entity không liên quan
* sửa Controller không liên quan
* đổi API
* đổi database schema
* đổi package structure
* refactor toàn project
* đổi naming convention
* đổi dependency
* đổi architecture

Nếu phát hiện vấn đề ngoài scope:

```text
1. Không tự ý sửa.
2. Ghi nhận vấn đề.
3. Báo cáo sau khi hoàn thành task.
```

---

# 10. ENTITY AND DATABASE

Không tự ý thay đổi Entity hoặc database schema nếu task không yêu cầu.

Khi thay đổi Entity:

Phải kiểm tra:

```text
Relationship
Nullable
Default value
Enum
Index
Unique constraint
Column type
Foreign key
Cascade
Lazy/Eager loading
Audit fields
Migration
DTO
Mapper
Repository
Service
```

Không chỉ sửa Entity rồi bỏ qua các layer liên quan.

Database migration phải được xử lý có chủ đích.

Không sử dụng:

```text
ddl-auto=create
ddl-auto=create-drop
```

trong production.

---

# 11. API DESIGN

API phải nhất quán với API hiện tại.

Trước khi tạo endpoint mới:

1. Kiểm tra endpoint tương tự.
2. Kiểm tra naming convention.
3. Kiểm tra request/response format.
4. Kiểm tra authentication.
5. Kiểm tra authorization.
6. Kiểm tra exception handling.

Không tạo duplicate endpoint.

Ví dụ nếu đã có:

```text
GET /api/products/{id}
```

không tạo thêm:

```text
GET /api/product/detail/{id}
```

nếu không có requirement rõ ràng.

---

# 12. VALIDATION

Input từ client phải được validate.

Kiểm tra:

```text
Required fields
Length
Format
Range
Enum
File type
File size
Business constraints
```

Không tin dữ liệu từ frontend.

Backend phải tự validate.

Frontend validation chỉ giúp UX, không thay thế backend validation.

---

# 13. SECURITY

Security phải được xem xét trong mọi feature.

Không:

* hard-code password
* hard-code API key
* hard-code JWT secret
* commit credentials
* log password
* log token
* log sensitive information

Credentials phải sử dụng environment/configuration.

Ví dụ:

```env
DB_USERNAME=
DB_PASSWORD=

JWT_SECRET=

CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

MAIL_USERNAME=
MAIL_PASSWORD=
```

---

# 14. AUTHENTICATION & AUTHORIZATION

Không chỉ kiểm tra user đã đăng nhập.

Phải phân biệt:

```text
Authentication
    ↓
Who are you?

Authorization
    ↓
What are you allowed to do?
```

API phải kiểm tra:

```text
Authenticated user
Role
Permission
Resource ownership
```

Không assume:

```text
ROLE_ADMIN = permission to do everything
```

nếu hệ thống có permission chi tiết.

Không expose chức năng admin cho customer chỉ vì frontend đã ẩn button.

Backend phải enforce permission.

---

# 15. ERROR HANDLING

Không trả stack trace hoặc exception nội bộ cho client.

Sử dụng exception handling tập trung nếu project đã có.

Response lỗi nên nhất quán.

Ví dụ:

```json
{
  "success": false,
  "message": "Product not found",
  "code": "PRODUCT_NOT_FOUND"
}
```

Không trả:

```text
SQL exception
Stack trace
Database credentials
Internal paths
Infrastructure secrets
```

---

# 16. TRANSACTION

Khi một nghiệp vụ thực hiện nhiều thao tác database liên quan, phải xem xét transaction.

Ví dụ:

```text
Create Order
    ↓
Create Order Items
    ↓
Update Inventory
    ↓
Create Payment Record
```

Nếu nghiệp vụ yêu cầu atomicity, phải đảm bảo transaction phù hợp.

Không sử dụng `@Transactional` một cách máy móc cho mọi method.

---

# 17. EXTERNAL SERVICES

Khi gọi:

```text
Cloudinary
SMTP
Payment Gateway
Shipping API
External API
```

phải xử lý:

```text
Timeout
Connection failure
Invalid response
Authentication failure
Rate limit
Retry where appropriate
```

Không để external service exception làm lộ thông tin nội bộ.

Không retry vô hạn.

---

# 18. FILE HANDLING

Đối với upload/download:

Phải kiểm tra:

```text
File size
MIME type
Extension
Filename
Path traversal
Storage destination
Access permission
```

Không tin filename do client gửi.

Không cho phép user truy cập arbitrary filesystem path.

---

# 19. FRONTEND / WEBSITE RULES

Khi phát triển website/frontend:

## Reuse

Trước khi tạo component:

```text
Button
Modal
Form
Table
Pagination
Input
Select
Card
Loading
Empty State
Error State
```

phải kiểm tra component hiện tại.

Nếu đã có component dùng chung:

**Tái sử dụng.**

Không tạo:

```text
ProductButton
OrderButton
CustomerButton
```

nếu chỉ khác styling nhỏ và có thể dùng component chung.

---

## UI consistency

Tuân thủ design system hiện tại:

```text
Typography
Spacing
Colors
Border radius
Buttons
Forms
Tables
Modal
Responsive behavior
```

Không tự ý tạo style hoàn toàn khác cho một page.

---

## Responsive

Website phải xem xét:

```text
Desktop
Tablet
Mobile
```

Không chỉ kiểm tra màn hình desktop.

---

## Loading State

API call phải có trạng thái:

```text
Loading
Success
Error
Empty
```

Không để UI đứng im khi request đang chạy.

---

## Error State

Không chỉ:

```text
console.error(error)
```

Mà phải có UX phù hợp để user biết thao tác thất bại.

---

## Form

Form phải:

* validate input
* disable submit khi đang submit nếu phù hợp
* hiển thị validation error
* xử lý API error
* tránh double submit

---

## API calls

Không gọi API trực tiếp ở quá nhiều component nếu project đã có:

```text
API client
Service layer
Hook
Repository
```

Phải sử dụng abstraction hiện tại.

Không duplicate Axios/fetch configuration.

---

# 20. PERFORMANCE

Không tối ưu sớm một cách cực đoan.

Nhưng phải tránh các lỗi rõ ràng:

```text
N+1 queries
Repeated API requests
Unnecessary rendering
Large unnecessary payloads
Loading huge datasets
Duplicate database queries
```

Frontend:

* pagination cho dataset lớn
* debounce search nếu cần
* lazy loading khi phù hợp
* tránh render lại không cần thiết

Backend:

* pagination
* projection/DTO khi cần
* fetch strategy hợp lý
* index database khi có lý do
* tránh query dư thừa

---

# 21. LOGGING

Log phải phục vụ debugging và monitoring.

Nên log:

```text
Important business events
External service failures
Unexpected exceptions
Important state transitions
```

Không log:

```text
Password
JWT
API Secret
SMTP Password
Sensitive personal information
```

Không sử dụng `System.out.println()` trong production code.

Sử dụng logging framework hiện tại của project.

---

# 22. CONFIGURATION

Configuration phải nằm ở configuration/environment.

Không hard-code:

```text
URL
Port
Password
Secret
API key
Timeout
Environment-specific value
```

Ví dụ:

```text
application.yaml
.env
.env.example
```

`.env.example` chỉ chứa placeholder.

Không commit `.env` chứa secret thật.

---

# 23. DEPENDENCIES

Trước khi thêm dependency:

1. Kiểm tra `pom.xml` / package manager.
2. Kiểm tra project đã có thư viện tương đương chưa.
3. Nếu có thì tái sử dụng.
4. Chỉ thêm dependency khi thực sự cần.
5. Kiểm tra compatibility với version hiện tại.

Không thêm thư viện chỉ để giải quyết một vấn đề đơn giản có thể xử lý bằng code hiện tại.

---

# 24. TESTING

Feature mới phải có test phù hợp.

Ưu tiên:

```text
Unit Test
Integration Test
API Test
```

Không cần test mọi getter/setter đơn giản.

Các logic quan trọng phải được test:

```text
Authentication
Authorization
Order calculation
Promotion
Payment
Inventory
Validation
External integration
```

Khi sửa code:

```text
Existing tests must continue passing.
```

Không sửa test chỉ để làm cho test pass nếu implementation mới đang sai.

---

# 25. BACKWARD COMPATIBILITY

Khi sửa code hiện tại:

Phải kiểm tra:

```text
Existing API
Existing callers
Existing services
Existing frontend
Existing tests
Existing database
```

Không phá behavior hiện tại nếu requirement không yêu cầu breaking change.

Nếu bắt buộc breaking change:

```text
Document the change.
Explain affected modules.
Update all affected callers.
Update tests.
```

---

# 26. REFACTORING

Refactor chỉ khi:

* task yêu cầu
* cần thiết để implement feature
* có bug rõ ràng
* duplicate gây ảnh hưởng nghiêm trọng

Không refactor toàn bộ project trong một task nhỏ.

Một task:

```text
"Add PDF helper"
```

không có nghĩa là:

```text
Rewrite OrderService
Rewrite Entity
Rewrite Controller
Rewrite architecture
```

---

# 27. GIT / CHANGE MANAGEMENT

Mỗi task phải tạo thay đổi có phạm vi rõ ràng.

Trước khi hoàn thành:

```text
Review changed files
Review git diff
Check unintended modifications
```

Nếu phát hiện file bị sửa ngoài scope:

```text
Revert it
```

trừ khi thay đổi đó thực sự cần thiết.

---

# 28. BEFORE CODING CHECKLIST

Trước khi code:

```text
[ ] Đọc requirement
[ ] Xác định scope
[ ] Đọc architecture
[ ] Tìm code tương tự
[ ] Tìm helper/util hiện có
[ ] Tìm service hiện có
[ ] Tìm DTO/model hiện có
[ ] Kiểm tra dependency
[ ] Kiểm tra configuration
[ ] Kiểm tra test hiện tại
```

---

# 29. DURING CODING CHECKLIST

Trong khi code:

```text
[ ] Tái sử dụng code hiện có
[ ] Không duplicate
[ ] Đúng layer
[ ] Đúng naming convention
[ ] Có comment/JavaDoc phù hợp
[ ] Không hard-code secret
[ ] Có validation
[ ] Có error handling
[ ] Không sửa ngoài scope
[ ] Không over-engineering
```

---

# 30. BEFORE FINISHING CHECKLIST

Trước khi báo hoàn thành:

```text
[ ] Review code
[ ] Review git diff
[ ] Kiểm tra duplicate
[ ] Kiểm tra security
[ ] Kiểm tra exception handling
[ ] Chạy compile
[ ] Chạy test
[ ] Kiểm tra configuration
[ ] Kiểm tra unintended changes
```

Backend nếu sử dụng Maven:

```bash
mvn -q -DskipTests clean compile
```

Sau đó chạy test phù hợp.

---

# 31. WHEN REQUIREMENT IS UNCLEAR

Không được tự ý suy đoán những phần có ảnh hưởng lớn đến:

```text
Database
Entity
API contract
Security
Business rules
Payment
Order state
Data migration
```

Nếu thiếu thông tin quan trọng:

1. Kiểm tra codebase để tìm context.
2. Nếu vẫn không xác định được, hỏi lại.
3. Không tự tạo business rule.

Đặc biệt:

> Không được tự ý "thiết kế giúp" một nghiệp vụ chưa được yêu cầu.

---

# 32. AI / CODE AGENT RULES

Khi AI được yêu cầu sửa code:

### Bước 1 — Understand

Đọc codebase và xác định implementation hiện tại.

### Bước 2 — Search

Tìm code có thể tái sử dụng.

### Bước 3 — Plan

Xác định file cần tạo/sửa.

### Bước 4 — Implement

Chỉ sửa đúng phạm vi.

### Bước 5 — Verify

Compile, test và review diff.

### Bước 6 — Report

Báo cáo:

```text
Files created
Files modified
Dependencies added
Configuration changed
Tests executed
Potential issues
```

---

# 33. GOLDEN RULE

## Before creating:

```text
SEARCH
```

## Before modifying:

```text
UNDERSTAND
```

## Before duplicating:

```text
REUSE
```

## Before refactoring:

```text
CHECK SCOPE
```

## Before finishing:

```text
TEST + REVIEW DIFF
```

---

# 34. Final Principle

Codebase này phải được phát triển theo hướng:

```text
Simple
Clean
Reusable
Maintainable
Secure
Testable
Consistent
Extensible
```

Không chạy theo việc tạo thật nhiều abstraction.

Không tạo class chỉ để "cho đẹp architecture".

Không duplicate code.

Không sửa code ngoài scope.

Không tự ý thay đổi business rule.

Không đánh đổi tính dễ bảo trì để lấy code ngắn hơn.

> **Hiểu codebase trước → tái sử dụng trước → thay đổi tối thiểu → kiểm tra sau khi thay đổi.**
