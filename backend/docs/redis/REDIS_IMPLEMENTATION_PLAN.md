# Redis Implementation Plan

> Trạng thái: **Phase 2 implemented cho các authorization READ API hiện hữu — các commerce READ API vẫn BLOCKED vì chưa tồn tại**  
> Ngày phân tích: 2026-08-28  
> Ngày triển khai Phase 1A: 2026-08-28  
> Ngày triển khai Phase 2: 2026-08-28  
> Phạm vi đã đọc: backend Spring Boot tại `backend/fashion-system`, schema/migration, controller, service, repository, entity, DTO, mapper, cấu hình, test và các file Docker/environment được track trong repository.  
> Approval: chủ dự án đã xác nhận Phase 1 và tiếp tục yêu cầu `Phase 2`.

## 1. Objective

Thiết kế một lớp Redis dùng chung cho nhiều module, ưu tiên cache-aside cho API LIST và DETAIL có tải đọc cao, nhưng PostgreSQL vẫn là source of truth.

Mục tiêu kỹ thuật:

- Giảm số query PostgreSQL và thời gian phản hồi cho dữ liệu đọc thường xuyên.
- Dùng một kiến trúc Redis chung; không tạo config riêng cho Product, Inventory, Category, Brand hoặc Promotion.
- Cache DTO/response ổn định, không cache JPA entity/proxy.
- TTL, cache name, key prefix, timeout và kill switch được cấu hình tập trung.
- Invalidation chỉ xảy ra sau khi transaction database commit thành công.
- Redis lỗi không làm hỏng API READ: log cảnh báo, đọc PostgreSQL và trả response.
- Không dùng Redis cho OTP hoặc Payment trong kế hoạch này.
- Không dùng Redis để thay đổi/lock/reserve tồn kho trong giai đoạn đầu.

Điều kiện quan trọng phát hiện từ codebase: các API Product, Category, Brand, Promotion, Inventory, Customer và Order chưa tồn tại ở controller/service. Vì vậy không thể gắn cache vào các endpoint này trong trạng thái hiện tại. Plan không giả định URL cho các API chưa có.

## 2. Current Redis Implementation

### 2.1 Thành phần đã có

- `pom.xml` đã khai báo `spring-boot-starter-data-redis`.
- `.gitignore` đã loại trừ `.env`.
- `application.yaml` đã import tùy chọn `.env`.
- `RateLimitFilter` có bộ đếm fixed-window bằng `ConcurrentHashMap` trong memory của từng application instance. Đây không phải Redis cache/distributed rate limit.

### 2.2 Thành phần chưa có tại thời điểm phân tích ban đầu

Qua tìm kiếm toàn repository không thấy:

- Redis properties trong `application.yaml` hoặc `.env.example`.
- `RedisConnectionFactory`, `RedisTemplate`, `StringRedisTemplate`.
- `RedisCacheManager`, `CacheManager`, `@EnableCaching`.
- `@Cacheable`, `@CachePut`, `@CacheEvict`.
- Redis repository, Redis service, utility hoặc common cache abstraction.
- Cấu hình serializer, TTL, timeout, error handler hoặc fallback.
- Lettuce/Jedis được khai báo trực tiếp. Client transitively resolved cần được xác nhận bằng dependency tree ở lúc triển khai; không nên thêm một client thứ hai nếu starter đã cung cấp client phù hợp.
- Unit/integration test liên quan Redis.
- Dockerfile, Docker Compose hoặc Redis container được khai báo trong repository. Yêu cầu cho biết local đang chạy Redis bằng Docker, nhưng cấu hình đó hiện nằm ngoài source control hoặc chưa được tạo.

Kết luận tại thời điểm phân tích: dependency Redis đã tồn tại, nhưng chưa có Redis implementation để tái sử dụng.

### 2.3 Cập nhật sau Phase 1A

Sau khi plan được phê duyệt, Phase 1A đã bổ sung:

- Một common `RedisCacheConfig`, cache-name registry và centralized properties.
- Redis connection qua environment, kill switch mặc định `REDIS_CACHE_ENABLED=false`.
- JSON serializer có whitelist type, key prefix versioned, TTL theo nhóm cache và SCAN batch clear.
- Transaction-aware cache manager và fallback error handler có rate-limited warning.
- Cache cho active modules, authorization module/group/permission LIST/DETAIL, catalog tree và role LIST/DETAIL.
- Invalidation cho module/group/permission/role CRUD và role-permission changes.
- Unit/service tests cho TTL/key/serializer, cache hit, invalidation và Redis failure fallback.
- Redis repository auto-scan được tắt vì project chỉ dùng Redis làm cache, không dùng RedisRepository.

Chưa có Redis Docker configuration được track và Docker daemon không hoạt động trong lúc verify, nên chưa chạy integration test với Redis container thật. Product/Category/Brand/Promotion cache chưa được triển khai vì endpoint vẫn chưa tồn tại.

### 2.4 Cập nhật sau Phase 2

Phase 2 chỉ triển khai trên các READ API thực sự tồn tại:

- Cache effective permissions theo `userId`, dùng chung cho permissions list/tree và permission check; không tạo cache riêng theo từng permission query.
- Tách computation sang `EffectivePermissionQueryService` để các call nội bộ trong `AuthorizationService` vẫn đi qua Spring cache proxy, tránh self-invocation bypass.
- Cache admin user-role list và user-permission override list theo `userId`.
- User role/permission mutations evict đúng cache của user đó. Role-permission và catalog metadata mutations clear toàn bộ effective-permission named cache để ưu tiên không giữ stale privilege; không dùng wildcard/Redis `KEYS`.
- TTL tập trung: effective permissions 30 giây, user assignment lists 1 phút; cả hai có environment override.
- Đã thêm test cache hit, serializer, TTL, targeted invalidation và revoke invalidation.

Inventory, Customer, User business profile và Order READ cache vẫn chưa được triển khai vì chưa có endpoint/service tương ứng. OTP và Payment không thay đổi.

## 3. Codebase Analysis

### 3.1 Kiến trúc hiện tại

- Spring Boot 4.1.0, Java 21, Spring MVC, Spring Data JPA và PostgreSQL.
- `BaseRepository<T, ID>` mở rộng `JpaRepository`, nên mọi repository có CRUD cơ bản (`findAll`, `findById`, `save`, `deleteById`).
- Có 6 controller và 12 file service/interface/record; API nghiệp vụ hiện tập trung vào authentication và authorization.
- Phần lớn domain commerce đã có Entity, DTO, Mapper và Repository nhưng chưa có Service/Controller.
- Không có pagination, full-text search, filter hoặc dynamic sort ở API backend hiện tại. LIST đang tồn tại đều trả `List` với sort cố định từ repository.
- Mapper tạo DTO tách khỏi entity; đây là điểm thuận lợi để cache DTO thay vì entity.

### 3.2 Inventory module theo code thực tế

| Domain | Thành phần dữ liệu hiện có | Service/Controller | Nhận xét |
|---|---|---|---|
| Product catalog | Product, ProductVariant, ProductImage, ProductAttribute, ProductTag, ProductTagMapping | Chưa có | Repository chỉ có CRUD cơ bản; chưa có query list/search/filter/detail aggregate. |
| Category | Category, cấu trúc `parentId` | Chưa có | Phù hợp read cache khi có API; thay đổi thấp nhưng cây category cần invalidation cả parent/children/list. |
| Brand | Brand | Chưa có | Phù hợp read cache khi có API. |
| Collection | Collection, liên kết `brandId` | Chưa có | Dữ liệu tham chiếu thay đổi thấp; candidate tốt sau khi có API. |
| Promotion | Promotion, Condition, Brand/Category/Collection/Product/Tier mappings, Usage | Chưa có | Promotion master có thể cache ngắn; usage/eligibility theo customer/order không nên cache chung. |
| Inventory | InventoryBalance, InventoryTransaction, StockReservation, GoodsReceipt/Item, GoodsIssue/Item | Chưa có | Schema phân biệt balance hiện tại, ledger transaction, reservation, nhập và xuất kho. Chưa có nghiệp vụ atomic/locking trong service/repository. |
| Customer | Customer, Address, SocialAccount, ActivityLog, Tier/Assignment, LoyaltyAccount/Transaction | Chỉ được dùng trong Auth | Chưa có Customer LIST/DETAIL API; chứa PII, cache phải user-scoped và dùng DTO an toàn. |
| User/HR | User, Department, UserDepartment, StoreStaff | Auth và authorization dùng một phần | Chưa có User LIST/DETAIL API; trạng thái active/locked được đọc mỗi request trong JWT filter. |
| Authorization | Module, PermissionGroup, Permission, Role, RolePermission, UserRole, UserPermission | Có đầy đủ API/service | Có LIST/DETAIL thật. Effective permissions chạy hai native join query cho mỗi lần nạp quyền của nhân viên. Candidate hiệu năng cao nhưng nhạy cảm bảo mật. |
| Order | Order, OrderItem, Address, StatusHistory, Promotion | Chưa có | Dữ liệu thay đổi thường xuyên theo lifecycle; chỉ cân nhắc TTL rất ngắn cho read UI sau khi có API. |
| Shipment/Refund | Shipment, Refund | Chưa có | Trạng thái thay đổi; ưu tiên DB, chỉ cache ngắn nếu đo được read-heavy. |
| Store/Supplier | Store, StoreStaff, Supplier | Chưa có | Store/Supplier reference list/detail là candidate tốt khi có API. |
| Notification | Notification, Template, Log, UserNotificationPreference | Chưa có | Template có thể cache; inbox/log có tính realtime và user-specific nên không ưu tiên. |
| Chat | InternalChatRoom/Message, OrderChatRoom/Message/Status, ChatAttachment | Chưa có | High-write/realtime; không phù hợp response cache thông thường. |
| Auth/audit/token | AuthAuditLog, RevokedToken, UserToken, social auth | Có Auth API | Không cache login/token revocation bằng read cache chung; stale state có rủi ro bảo mật. |
| OTP | PasswordResetOtp | Có API riêng | **Loại khỏi plan Redis theo yêu cầu.** |
| Payment | Payment, PaymentTransaction, PaymentWebhookLog | Chưa có API | **Loại khỏi plan Redis theo yêu cầu.** |

Frontend có màn hình/mock data cho nhiều domain commerce, nhưng đó không phải bằng chứng về backend API. Plan chỉ phân loại endpoint từ annotation controller backend.

### 3.3 Phân tích nghiệp vụ Inventory/Stock

Code hiện tại thể hiện:

- `inventory_balances` có khóa kép `(store_id, product_variant_id)` và ba số lượng `available_quantity`, `reserved_quantity`, `damaged_quantity`.
- `inventory_transactions` là lịch sử biến động theo variant/store, có type, reference và `balance_after`.
- `stock_reservations` gắn order/store/variant, quantity, status và `expired_at`.
- Goods receipt/issue và item đại diện luồng nhập/xuất kho.
- Database có foreign key và index cơ bản, nhưng chưa thấy service, transaction orchestration, pessimistic/optimistic locking, atomic update hay constraint chống số âm.

Phân biệt hai bài toán:

1. **READ cache**: chỉ phục vụ hiển thị tồn kho/availability. Giá trị có thể cũ vài giây, không được dùng làm quyết định cuối cùng khi checkout.
2. **Concurrency/atomic stock**: quyết định user nào mua được sản phẩm cuối, reserve/release/commit stock. Đây là correctness path và **không nằm trong Phase 1/2**.

Chưa có bất kỳ API `GET inventory`, `GET inventory/{id}`, `GET product/{id}/stock` hoặc `GET product/{id}/availability` trong code hiện tại. Khi API read được bổ sung, key phải chứa cả `storeId` và `productVariantId`; không dùng `productId` đơn lẻ nếu một product có nhiều variant/store.

## 4. Modules Suitable for Redis

### 4.1 Candidate tốt khi có API

- Product LIST/DETAIL: read-heavy storefront; detail có thể tổng hợp variants/images/attributes/tags nên giảm nhiều query.
- Category LIST/DETAIL/tree: thay đổi thấp, truy cập thường xuyên.
- Brand LIST/DETAIL và Collection LIST/DETAIL: dữ liệu tham chiếu thay đổi thấp.
- Promotion LIST/DETAIL master data: cache ngắn và phải tôn trọng mốc start/end.
- Store/Supplier/reference catalog: dữ liệu nhỏ, ít đổi.
- Module/permission catalog hiện có: dữ liệu metadata nhỏ, ít đổi, có write path rõ để invalidation.

### 4.2 Candidate có điều kiện

- Inventory availability display: TTL vài giây, invalidation theo variant/store sau mọi biến động stock; DB vẫn quyết định checkout.
- Customer/User admin LIST/DETAIL: chỉ cache DTO đã loại secret, key theo authorization scope, TTL ngắn và invalidation chính xác.
- Order LIST/DETAIL: chỉ cache khi profiling chứng minh read-heavy; TTL rất ngắn do status/payment/shipment/refund thay đổi.
- Effective permissions: có lợi lớn vì hiện được nạp khi xác thực mỗi employee request, nhưng stale permission có thể tiếp tục cấp quyền đã bị thu hồi. Chỉ triển khai sau targeted invalidation, after-commit event và security acceptance.

### 4.3 Không cache trong các phase read đầu

- OTP và Payment: loại khỏi phạm vi.
- Password hash, token, Redis-backed login session hoặc token revocation lookup.
- Promotion usage count/eligibility theo customer/order như một cache chung.
- Stock reservation/atomic decrement/increment/lock.
- Notification inbox/log, chat messages/status và activity/audit log.
- Inventory transaction ledger, goods receipt/issue đang thay đổi.
- Response chứa dữ liệu PII nếu key không bao gồm đúng principal/scope.

## 5. API Classification

### 5.1 API READ thực sự tồn tại

Tần suất là đánh giá định tính từ vị trí gọi và bản chất API; cần đo bằng metrics trước/sau triển khai.

| Module | API thực tế | Loại | Read Frequency | Data Change Frequency | Redis phù hợp? | Lý do |
|---|---|---:|---:|---:|---|---|
| Module metadata | `GET /api/modules` | LIST | High | Low | YES | Menu/module metadata cho UI, query sort cố định, public-to-authenticated read và write ít. Pilot an toàn nhất. |
| Authorization self | `GET /api/authorization/me/permissions` | LIST | High | Low/Medium | YES — Phase 2 | Hai join query và merge/sort được cache per user; TTL 30 giây và security invalidation bắt buộc. |
| Authorization self | `GET /api/me/permissions` | DETAIL/tree | High | Low/Medium | YES — Phase 2 | Dùng chung cached effective-permission value per user, không cache thêm tree trùng dữ liệu. |
| Authorization self | `GET /api/authorization/me/check?permissionCode=&scope=` | DETAIL/check | High | Low/Medium | YES — Phase 2 | Hưởng cache per-user; không tạo key riêng cho từng permission/scope check. |
| Authorization catalog | `GET /api/admin/authorization/modules` | LIST | Medium | Low | YES | Sorted list, thay đổi chỉ qua admin CRUD. |
| Authorization catalog | `GET /api/admin/authorization/modules/{id}` | DETAIL | Low/Medium | Low | YES | Targeted invalidation đơn giản. |
| Authorization catalog | `GET /api/admin/authorization/groups` | LIST | Medium | Low | YES | Reference data, write path rõ. |
| Authorization catalog | `GET /api/admin/authorization/groups/{id}` | DETAIL | Low/Medium | Low | YES | Targeted invalidation đơn giản. |
| Authorization catalog | `GET /api/admin/authorization/permissions` | LIST | Medium | Low | YES | Reference data, write path rõ. |
| Authorization catalog | `GET /api/admin/authorization/permissions/{id}` | DETAIL | Low/Medium | Low | YES | Targeted invalidation đơn giản. |
| Authorization catalog | `GET /api/admin/authorization/catalog` | LIST/tree | Medium/High | Low | YES | Thực hiện ba query cố định rồi dựng cây; cache giúp tránh lặp lại. |
| Role | `GET /api/admin/authorization/roles` | LIST | Medium | Low | YES | Sorted reference list, write path rõ. |
| Role | `GET /api/admin/authorization/roles/{roleId}` | DETAIL | Medium | Low/Medium | YES, conditional | Detail gồm role và permission join; phải invalidate khi grant/replace/remove permission. |
| User authorization | `GET /api/admin/authorization/users/{userId}/roles` | LIST | Low/Medium | Low/Medium | YES — Phase 2 | Cache per user TTL 1 phút; assign/replace/remove evict exact user. |
| User authorization | `GET /api/admin/authorization/users/{userId}/permissions` | LIST | Low/Medium | Low/Medium | YES — Phase 2 | Cache per user TTL 1 phút; override mutation evict exact user và effective permissions. |

`GET /api/modules` và admin `GET .../modules` có response/purpose khác nhau, nên không dùng chung cache entry dù cùng bảng.

### 5.2 Module được yêu cầu nhưng chưa có READ API

| Module | LIST API thực tế | DETAIL API thực tế | Redis decision hiện tại | Đánh giá khi API tồn tại |
|---|---|---|---|---|
| Product | Không có | Không có | BLOCKED | YES; ưu tiên cao. |
| Category | Không có | Không có | BLOCKED | YES; ưu tiên cao, TTL dài. |
| Brand | Không có | Không có | BLOCKED | YES; ưu tiên cao, TTL dài. |
| Promotion | Không có | Không có | BLOCKED | YES cho master data với TTL ngắn/boundary-aware. |
| Inventory/Stock | Không có | Không có | BLOCKED | TBD/YES cho display read vài giây; NO cho correctness path. |
| Customer | Không có | Không có | BLOCKED | TBD; PII/user scope và TTL ngắn. |
| User | Không có | Không có | BLOCKED | TBD; admin/security state nhạy cảm. |
| Order | Không có | Không có | BLOCKED | Thường NO/TBD; thay đổi cao, chỉ cache ngắn nếu đo có lợi. |
| Collection | Không có | Không có | BLOCKED | YES; reference data. |
| Product Variant/Tag | Không có | Không có | BLOCKED | YES nếu đọc độc lập; thường là dependency của Product detail. |
| Store/Supplier | Không có | Không có | BLOCKED | YES; reference data. |
| Shipment/Refund | Không có | Không có | BLOCKED | TBD; trạng thái đổi thường xuyên. |
| Loyalty/Tier | Không có | Không có | BLOCKED | Tier master có thể cache; account/transaction không ưu tiên. |
| Notification | Không có | Không có | BLOCKED | Template YES; inbox/log NO. |
| Chat | Không có | Không có | BLOCKED | NO cho response cache thông thường. |

Không có API LIST nào nhận page/filter/search/sort trong backend hiện tại. Convention query hash bên dưới là thiết kế trước cho lúc các API commerce được triển khai, không mô tả hành vi đang có.

## 6. Redis Architecture

Mô hình đề xuất:

```text
Controllers
    -> Module services / future commerce services
        -> Spring Cache abstraction (cache-aside)
            -> one shared RedisCacheManager + serializer + error handler
                -> Redis
        -> PostgreSQL repositories (source of truth / fallback)
```

### 6.1 Common Redis layer

Một package config/cache dùng chung nên sở hữu:

- Redis connection properties lấy từ environment.
- Một connection factory/client do Spring Boot quản lý.
- Một `RedisCacheManager` với default policy an toàn và TTL override theo cache name.
- Central cache-name constants/registry để tránh typo.
- Central cache properties validated lúc startup.
- Cache error handler cho GET/PUT/EVICT/CLEAR.
- Key generation/canonical query hashing dùng chung cho LIST.
- Logging/metrics chuẩn và kill switch `REDIS_CACHE_ENABLED`.

Không tạo `ProductRedisConfig`, `InventoryRedisConfig`, `CategoryRedisConfig`, v.v. Module chỉ khai báo cache name/key và phát invalidation event sau commit.

### 6.2 Cache abstraction choice

- Mặc định dùng Spring Cache cho LIST/DETAIL cache-aside vì use case đơn giản và nhiều module dùng chung.
- Chỉ dùng `RedisTemplate`/Lua ở Phase 3 cho thao tác Redis nâng cao cần atomicity; không đưa vào read cache nếu Spring Cache đáp ứng được.
- Tránh self-invocation làm annotation cache không chạy. Cache boundary phải ở public service/proxy riêng và được test.
- Chỉ cache DTO/immutable response. Không cache entity Hibernate, lazy proxy, password hash hoặc object chứa credential.

### 6.3 Serializer

- Key: string UTF-8.
- Value: JSON serializer dùng ObjectMapper tương thích với Spring Boot/Jackson hiện tại, hỗ trợ UUID, enum và Java Time.
- Không dùng Java native serialization.
- Không bật polymorphic default typing rộng/untrusted. Nếu serializer cần type metadata, giới hạn type cho DTO package hoặc dùng typed cache serializer.
- Dùng namespace/schema version để deploy DTO breaking change không đọc nhầm payload cũ.

### 6.4 Transaction boundary

- Database write hoàn tất trước; invalidation chạy `AFTER_COMMIT`.
- Transaction rollback không được evict cache hợp lệ.
- Nếu invalidation Redis lỗi, DB write vẫn thành công; log/metric và TTL ngắn giới hạn stale window. Với dữ liệu security/inventory phải có retry/outbox hoặc không cache cho tới khi cơ chế đủ an toàn.

## 7. Cache Key Convention

Format vật lý:

```text
fs:{environment}:v1:{domain}:{view}::{logical-key}
```

Ví dụ thiết kế (chưa phải API/implementation hiện hữu):

```text
fs:prod:v1:product:detail::{productId}
fs:prod:v1:product:list::{sha256(canonicalQuery)}
fs:prod:v1:category:detail::{categoryId}
fs:prod:v1:category:list::{sha256(canonicalQuery)}
fs:prod:v1:inventory:availability::{storeId}:{productVariantId}
fs:prod:v1:authorization.effective.permissions::{userId}
fs:prod:v1:authorization.user-role.list::{userId}
fs:prod:v1:authorization.user-permission.list::{userId}
```

Quy tắc:

- `fs` ngăn xung đột với ứng dụng khác dùng chung Redis.
- `environment` (`local`, `staging`, `prod`) ngăn môi trường đọc chéo key; production tốt nhất vẫn dùng instance/database riêng.
- `v1` cho phép cache schema migration/flush logic bằng đổi prefix.
- `domain:view` tạo namespace có thể clear độc lập.
- Detail dùng UUID/code canonical rõ ràng.
- LIST dùng SHA-256 của canonical query để key ngắn, cố định và không lộ search text/PII.
- Canonical query phải chuẩn hóa default, lowercase/trim nếu semantics cho phép, page, size, filter, sort direction, locale, visibility, principal/tenant scope. Sort field order phải cố định.
- Không dùng raw `toString()` của request/map vì thứ tự field và format không ổn định.
- Không đưa password, token, email/phone raw hoặc secret vào key/log.

Với project hiện tại, LIST không có query params nên logical key dùng `all` hoặc version cố định. Khi có filter/pagination mới chuyển sang query hash.

## 8. TTL Strategy

TTL được bind từ một configuration object/map tập trung theo cache name, không hard-code trong service.

| Cache dự kiến | TTL khởi điểm | Lý do |
|---|---:|---|
| Active module list | 10 phút | Metadata nhỏ, ít đổi, có admin invalidation. |
| Authorization catalog list/tree/detail | 2–5 phút | Ít đổi nhưng thay đổi ảnh hưởng UI/quyền; invalidation sau commit bắt buộc. |
| Role list/detail | 1–2 phút | Grant có thể thay đổi; TTL ngắn hơn catalog. |
| Effective permissions per user | 30 giây (đã triển khai) | Tải đọc rất cao nhưng stale grant/revoke là rủi ro security; targeted user eviction hoặc safe namespace clear là chính, TTL là safety net. |
| User role/permission assignment lists | 1 phút (đã triển khai) | Admin read theo user, thay đổi không thường xuyên và có exact-user invalidation. |
| Product detail | 5 phút | Read-heavy, dữ liệu medium-change; invalidate qua product/dependencies. |
| Product list/search | 1–2 phút | Cardinality cao và giá/filter/status đổi; clear list namespace khi write. |
| Category detail/list/tree | 15–30 phút | Reference data thay đổi thấp. |
| Brand detail/list | 15–30 phút | Reference data thay đổi thấp. |
| Collection/Store/Supplier/Tag | 10–30 phút | Reference data thay đổi thấp. |
| Promotion master detail/list | 30–60 giây | Active state phụ thuộc thời gian và usage; TTL không được vượt qua mốc start/end gần nhất. |
| Inventory display availability | 2–5 giây | Dữ liệu thay đổi cao; chỉ phục vụ UI và evict sau mutation. |
| Customer/User sanitized detail/list | 30–60 giây | PII/security state và write frequency cao hơn catalog. |
| Order detail/list | 5–15 giây hoặc không cache | Status/payment/shipment/refund đổi thường xuyên. |

Không cache khi:

- Response phụ thuộc dữ liệu realtime/correctness như checkout stock decision.
- Query có cardinality rất lớn hoặc gần như không lặp lại.
- Response chứa PII mà key/scope không bảo đảm isolation.
- Invalidation dependencies chưa được xác định.
- Payload quá lớn so với lợi ích query.
- Promotion sắp đổi trạng thái mà TTL tĩnh vượt mốc thời gian; dùng dynamic TTL/bypass hoặc TTL bằng thời gian tới boundary gần nhất.

TTL trên là baseline để load test, không phải giá trị cuối. Theo dõi hit ratio, stale rate, latency và memory rồi điều chỉnh.

## 9. Cache Invalidation Strategy

### 9.1 Chiến lược LIST hiện tại

Ở quy mô code hiện tại, ưu tiên **clear toàn bộ named list cache** của domain sau write thay vì xóa wildcard `products:*`:

- Không chạy Redis `KEYS` trong production.
- `allEntries/clear` chỉ tác động namespace cache đã định nghĩa; cấu hình clear phải dùng SCAN/batch strategy nếu phiên bản Spring Data Redis hỗ trợ.
- Khi cardinality LIST tăng lớn, Phase 3 chuyển sang generation/version key: tăng generation O(1), key cũ tự hết TTL.

Không cố targeted invalidation từng query hash ở Phase 1 vì một update có thể ảnh hưởng nhiều tổ hợp filter/search/sort/page và dễ bỏ sót.

### 9.2 Dependency matrix

| Write thành công | Detail cần evict | List/tree cần clear | Cross-domain cần xem xét |
|---|---|---|---|
| Module create/update/delete | Module detail | active modules, admin modules, catalog tree | Effective permission của mọi user nếu `active`, code/name/sort metadata ảnh hưởng response. |
| PermissionGroup create/update/delete | Group detail | groups, catalog tree | Permission detail/list nếu response embed group; effective permission affected users nếu metadata embed trong response. |
| Permission create/update/delete | Permission detail | permissions, catalog tree | Role detail và effective permissions của affected users. |
| Role create/update/delete | Role detail | roles | User role lists/effective permissions cho user được gán role. |
| Role permission assign/replace/remove | Role detail | — | Clear toàn bộ effective-permission named cache ở quy mô hiện tại để không bỏ sót affected user; có thể tối ưu sang affected-user IDs khi profiling chứng minh cần. |
| User role assign/replace/remove | User authorization detail | user role list | Effective permission key của chính user. |
| User permission assign/replace/remove | User authorization detail | user permission list | Effective permission key của chính user. |
| Product create | — | Product lists | Có thể clear promotion/product-picker views. |
| Product update/delete | Product detail | Product lists | Evict composite views; delete có thể evict negative cache nếu dùng sau này. |
| Variant/image/attribute/tag mapping write | Parent Product detail | Product lists nếu response/filter phụ thuộc | Inventory availability key nếu variant bị disable/delete. |
| Category write | Category detail + affected parent/children | Category list/tree | Product lists/details nếu response/filter embed category. |
| Brand write | Brand detail | Brand list | Product/Collection list/detail nếu embed brand. |
| Collection write | Collection detail | Collection list | Product lists/details nếu embed collection. |
| Promotion hoặc target mapping write | Promotion detail | Promotion lists | Product views nếu hiển thị effective promotion. |
| Promotion usage write | Không cache usage | Active promotion result nếu result phụ thuộc quota | Không cache eligibility chung. |
| Inventory balance/receipt/issue/reservation write | Exact `(storeId, variantId)` availability | Inventory lists/low-stock views | Product availability aggregate của product chứa variant. |
| Customer/User write | Exact sanitized detail | List theo scope | Auth/security caches phải evict nếu active/locked/role state đổi. |
| Order/status/shipment/refund write | Exact order detail | Customer/admin order lists | Dashboard/count caches nếu có sau này. Payment cache không nằm trong scope. |

Product/Category/Brand/Promotion rows trong bảng trên là thiết kế cho API tương lai; không có write service hiện tại để gắn invalidation.

### 9.3 Write ordering

```text
DB transaction -> COMMIT thành công -> publish/handle invalidation -> request tiếp theo MISS -> DB -> cache
```

Không dùng cache write-through làm source of truth. Create/update/delete thất bại hoặc rollback không thay đổi cache.

## 10. Redis Failure Strategy

### 10.1 Cache-aside flow

```text
GET -> Redis hit -> return DTO
GET -> Redis miss -> PostgreSQL -> map DTO -> best-effort Redis SET -> return DTO
GET -> Redis error -> warning/metric -> PostgreSQL -> return DTO
```

Redis không được là single point of failure cho read cache.

### 10.2 Failure handling

| Failure | Hành vi đề xuất |
|---|---|
| Connection timeout/unavailable | Timeout thấp, CacheErrorHandler bỏ qua cache operation, rate-limited warning, fallback DB. |
| Redis restart/cache loss | Xem như cold cache; tự populate dần từ DB, không cần restore cache. |
| Read timeout | Bỏ qua cache cho request, không retry nhiều lần trong request. |
| SET failure | Vẫn trả DB response; log/metric, không fail request. |
| EVICT/CLEAR failure | DB write vẫn là source of truth; log ở mức error/warning có alert, TTL giới hạn stale. Security/inventory cache cần retry/outbox hoặc không bật. |
| Serialization/deserialization | Xem entry là miss; best-effort xóa key hỏng; fallback DB; tăng cache schema version khi DTO breaking change. |
| Redis chập chờn kéo dài | Kill switch/bypass cache; cân nhắc circuit breaker ở phase sau để tránh mỗi request chờ timeout. |
| DB cũng lỗi | Trả lỗi DB theo hành vi hiện tại; không coi stale Redis là authoritative fallback mặc định. |

Logging phải có operation, cache name, exception class, latency/correlation ID; không log payload, credential hoặc PII key. Cần metric hit/miss/error/eviction failure, Redis latency và DB fallback rate khi observability stack được chọn.

## 11. Local Docker Configuration

Repository hiện không có Docker Compose/Dockerfile/Redis config. Do đó chưa thể “tái sử dụng Redis container hiện tại” từ source control; local Redis Docker được mô tả trong yêu cầu có thể đang được chạy thủ công.

Kế hoạch local sau khi được duyệt:

- Xác minh tên/network/port của container Redis đang dùng trước khi thay đổi Docker.
- Nếu project sau đó có Compose chung, thêm Redis service vào chính file đó, không tạo container trùng.
- Map local host port theo environment; mặc định dự kiến `localhost:6379`, không hard-code trong Java.
- Spring Boot chạy trên host kết nối `REDIS_HOST=localhost`; nếu app chạy trong Compose thì dùng service DNS, không dùng `localhost`.
- Healthcheck/persistence là tùy mục tiêu local; read cache không cần persistence để correctness.
- Không sửa Docker trong planning stage này.

## 12. Production Configuration

```text
Local:      Spring Boot -> Docker Redis
Production: Spring Boot -> private managed/self-hosted Redis service
```

Kế hoạch:

- Cấu hình hoàn toàn bằng environment/secret manager.
- Tách Redis cache theo environment và giới hạn memory/eviction policy phù hợp.
- Dùng TLS nếu provider hỗ trợ/yêu cầu.
- Redis production nằm trên private network/security group; không expose public nếu không cần.
- Đặt connect/read timeout thấp hơn SLA API để fallback DB còn đủ thời gian.
- Capacity plan dựa trên serialized payload size, key cardinality, TTL và peak traffic.
- Chọn eviction policy cho workload cache (ví dụ allkeys-LRU/LFU theo provider) sau load test; cache loss phải an toàn.
- Deployment thay đổi DTO/cache schema dùng prefix version mới, tránh deserialize key cũ.

## 13. Environment Variables

Khi triển khai mới cập nhật `.env.example`; không sửa ở bước plan.

Biến dự kiến tối thiểu:

```text
REDIS_CACHE_ENABLED=
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=
REDIS_SSL_ENABLED=
REDIS_CONNECT_TIMEOUT_MS=
REDIS_READ_TIMEOUT_MS=
REDIS_KEY_PREFIX=
```

- Chưa thêm `REDIS_USERNAME` vì chưa chọn provider và codebase hiện không cho thấy nhu cầu ACL username. Chỉ thêm nếu provider thực tế yêu cầu.
- Có thể thêm database index/pool settings sau khi xác nhận topology; không mặc định dùng database index để thay thế isolation production.
- TTL module nên bind dưới một prefix tập trung, ví dụ `redis.cache.ttl.*`, với default và override theo cache name; tên biến cụ thể chốt trong Phase 1 design review.
- `.env.example` chỉ chứa placeholder/default local không nhạy cảm. Secret thật nằm trong `.env` local hoặc production secret manager.

## 14. Security Considerations

- Không commit Redis password/credential; không hard-code host/password/secret trong Java.
- `.env` tiếp tục bị ignore; `.env.example` không chứa secret thật.
- Không đưa token, password hash, OTP, raw email/phone hoặc payload PII vào Redis key/log.
- Cache Customer/User chỉ chứa sanitized DTO và phải phân vùng theo principal/authorization scope.
- Effective-permission cache dùng TTL 30 giây và bắt buộc invalidation ở mọi role/user permission write path; thay đổi role grant/catalog metadata ưu tiên clear named cache an toàn để tránh privilege retention.
- Redis production không public; giới hạn network chỉ application/service được phép truy cập.
- Dùng TLS và credential rotation nếu provider hỗ trợ.
- Hạn chế Redis ACL chỉ các command cần cho cache; không cấp quyền quản trị rộng cho app nếu provider hỗ trợ ACL.
- Không expose Redis diagnostics/key dump qua API.
- JSON deserialization không cho phép arbitrary polymorphic class instantiation.

## 15. Testing Strategy

### 15.1 Unit tests theo cached service

- Cache hit: repository không được gọi, trả đúng DTO.
- Cache miss: repository gọi đúng một lần, response được cache.
- Redis GET failure: repository vẫn được gọi và response thành công.
- Redis SET failure: response DB vẫn thành công.
- Not found/null: xác nhận policy không cache null ở phase đầu.
- Key generation: cùng semantic query tạo cùng hash; filter/page/sort/locale/scope khác tạo key khác.
- Serializer round-trip cho UUID, enum, `LocalDate`, `LocalDateTime`, nested DTO/list/page response.

### 15.2 Invalidation tests

- CREATE commit thành công -> clear LIST cache; rollback -> không clear.
- UPDATE -> evict detail + list/dependent views; GET kế tiếp query DB và repopulate.
- DELETE -> evict detail/list; không trả object đã xóa.
- Dependency writes (variant/image/category/brand/promotion target) evict đúng Product composite cache khi các API này tồn tại.
- Role/user permission changes evict đúng effective permission key; test revoke/DENY quan trọng hơn hit ratio.
- Inventory write evict chính xác `(storeId, variantId)` display cache; checkout test luôn đọc authoritative path.

### 15.3 Integration tests

- Dùng PostgreSQL + Redis Testcontainers nếu CI có Docker; dependency Testcontainers chỉ thêm sau approval.
- Thử Redis stop/restart giữa request để chứng minh fallback và cold-cache recovery.
- Kiểm tra TTL thực tế, key prefix, isolation environment và namespace clear.
- Concurrency test cache miss để đo stampede (chỉ quan sát ở Phase 1, chưa thêm distributed lock).
- Contract test bảo đảm cached và uncached response JSON giống nhau.

### 15.4 Manual Redis CLI

- Xác nhận MISS -> SET -> HIT bằng repository query count/log có kiểm soát.
- Kiểm tra `TTL`, prefix, serialized value không chứa secret.
- Update/delete DB qua API rồi xác nhận key detail/list bị invalidated.
- Dừng container Redis và gọi GET: API vẫn trả từ PostgreSQL, log cảnh báo không lộ secret.
- Không dùng `KEYS *` trên production; dùng `SCAN` có prefix cho chẩn đoán có kiểm soát.

## 16. Phase 1

### Phase 1A — Common read-cache foundation và API đang tồn tại

Sau approval:

1. Chốt common configuration, serializer, cache registry, centralized TTL, key prefix, timeout, error handler và kill switch.
2. Xác minh dependency/client compatibility với Spring Boot 4.1.0; không thêm dependency Redis trùng.
3. Pilot `GET /api/modules`.
4. Nếu pilot đạt tiêu chí, cache các API catalog thực tế: admin modules/groups/permissions list/detail và catalog tree.
5. Cache role list/detail sau khi hoàn thiện invalidation cho role-permission mutation.
6. Phase 1 chưa cache effective permission per user; hạng mục này đã qua gate và được triển khai ở Phase 2 với revoke/DENY invalidation tests.
7. Thêm unit/integration/failure/invalidation tests và metrics/logging tối thiểu.

### Phase 1B — Business catalog ưu tiên, chỉ khi API tồn tại

Product, Category, Brand và Promotion LIST/DETAIL là ưu tiên nghiệp vụ, nhưng hiện chưa có controller/service. Redis work không được tự tạo các API đó ngoài scope. Khi API được triển khai và contract được duyệt, đưa lần lượt vào Phase 1 theo thứ tự:

1. Product LIST/DETAIL.
2. Category LIST/DETAIL/tree.
3. Brand LIST/DETAIL.
4. Promotion LIST/DETAIL master data với time-bound TTL.

Gate cho từng API: endpoint thật, DTO response ổn định, query/filter/sort đã xác định, write paths và dependency invalidation đầy đủ, benchmark chứng minh lợi ích.

## 17. Phase 2

Đã triển khai candidate có API thật và call frequency cao:

- Effective permission cache per user cho `GET /api/authorization/me/permissions`, `GET /api/me/permissions`, `GET /api/authorization/me/check` và authorization checks nội bộ.
- User role list cho `GET /api/admin/authorization/users/{userId}/roles`.
- User direct-permission list cho `GET /api/admin/authorization/users/{userId}/permissions`.
- Complete invalidation trên write paths hiện hữu: exact-user eviction cho direct assignment; safe all-entry eviction cho role grant và catalog metadata.

Các candidate còn lại chỉ triển khai khi có API thật và profiling cho thấy có lợi:

- Inventory display READ cache theo store + product variant, TTL 2–5 giây; không dùng ở checkout correctness path.
- Collection, Product Variant/Tag, Store, Supplier và các reference READ khác.
- Customer/User sanitized LIST/DETAIL với principal/scope-aware key và PII review.
- Order LIST/DETAIL TTL rất ngắn nếu read-heavy; ưu tiên targeted eviction theo order/customer/status.
- Tier/template/reference master data; không cache transaction/log/inbox/chat streams.

Mỗi module cần đo baseline query count/p95, estimate cardinality và xác nhận write dependency trước khi bật.

## 18. Phase 3

Chỉ là future plan, chưa triển khai:

- Inventory atomic operation: cân nhắc PostgreSQL atomic update/locking trước; Redis chỉ dùng nếu thiết kế source of truth/reconciliation rõ ràng.
- Stock reservation lifecycle: reserve, expire, release, commit và recovery/reconciliation.
- Distributed lock: chỉ cho critical section cụ thể, có lease/timeout/fencing; không coi lock đơn giản là bảo đảm chống overselling.
- Idempotency cho order/inventory command, không bao gồm Payment.
- Cache stampede protection: single-flight, distributed lock per hot key, cache warm-up, TTL jitter hoặc probabilistic early expiration.
- Rate limiting phân tán: thay `ConcurrentHashMap` hiện tại khi chạy nhiều instance; OTP/Payment vẫn ngoài phạm vi thay đổi của task này.
- List generation/version invalidation và background cleanup.
- Cache warming cho hot catalog sau deploy nếu metrics chứng minh cần.

### Future Redis Strategy cho Inventory concurrency

Tình huống hai user mua sản phẩm cuối phải được giải quyết ở command/write path, không dựa vào read cache. Trước khi chọn Redis cần thiết kế:

- PostgreSQL source of truth và invariant `available/reserved >= 0`.
- Atomic conditional update hoặc row lock/optimistic version trong một transaction.
- Idempotency key của order command.
- Reservation TTL và worker/recovery khi service crash.
- Nếu Redis tham gia: atomic Lua/function, fencing/version, durable event/reconciliation với PostgreSQL, failure mode khi Redis/DB commit lệch nhau.
- Load/concurrency/chaos tests chứng minh không oversell.

Không thực hiện bất kỳ phần nào ở trên trong Phase 1/2 read cache.

## 19. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Priority commerce APIs chưa tồn tại | Không thể gắn Product/Category/Brand/Promotion cache | Không invent endpoint; hoàn thiện API contract trước rồi kích hoạt Phase 1B. |
| Cache stale sau write | Trả dữ liệu cũ | AFTER_COMMIT invalidation, TTL safety net, dependency matrix test. |
| Eviction Redis lỗi | DB đúng nhưng cache cũ còn tồn tại | Alert/metric, TTL ngắn, retry/outbox cho security/inventory hoặc không cache. |
| Effective permission stale | User giữ quyền đã bị thu hồi | Không cache ban đầu; targeted affected-user eviction + 15–30s TTL + revoke tests. |
| Inventory cache được dùng sai | Overselling/quyết định checkout sai | Tách display query khỏi authoritative command; naming/documentation rõ. |
| Query-hash cardinality cao | Redis memory tăng, hit ratio thấp | Normalize params, giới hạn page/size/filter, TTL ngắn, metric cardinality, bypass low-reuse query. |
| LIST invalidation dùng wildcard | Redis blocking/latency | Named cache clear với SCAN hoặc generation token; không dùng `KEYS`. |
| Serialization/DTO change | Deserialization failure | JSON DTO, schema version prefix, compatibility test. |
| Cache annotation self-invocation | Cache không hoạt động dù code có annotation | Đặt boundary qua Spring proxy, integration test query count. |
| Redis outage tạo DB surge | PostgreSQL quá tải | Timeout thấp, kill switch/circuit breaker, capacity headroom, staggered TTL/warm-up. |
| Cache stampede khi TTL hết | Nhiều query DB đồng thời | Quan sát Phase 1; Phase 3 single-flight/lock/jitter/early refresh. |
| PII cross-user leak | Security/privacy incident | Scope/principal trong key, sanitized DTO, security tests, không log payload. |
| Boot 4.1/client API compatibility | Build/runtime issue | Xác minh resolved dependency và APIs ở đầu implementation; dùng starter-managed client. |
| Local Docker config không được track | Onboarding không tái lập | Xác minh container hiện tại; sau approval tài liệu hóa/đưa vào compose chung, không tạo trùng. |

## 20. Future Improvements

- Metrics dashboard cho hit ratio, miss, Redis error, DB fallback, key count, memory và p95 latency theo cache name.
- Dynamic TTL theo promotion boundary và payload volatility.
- Near-cache/local cache chỉ khi multi-instance invalidation được giải quyết; chưa cần ở hiện tại.
- Versioned list generations để invalidation O(1).
- Hot-key detection và selective warm-up.
- Compression chỉ với payload lớn và sau benchmark CPU/network trade-off.
- Read-through refresh/soft TTL cho catalog cực nóng.
- Resilience circuit breaker để tạm bypass Redis khi outage kéo dài.
- Capacity/eviction alert và runbook cho Redis restart, memory pressure, serializer error.
- Đánh giá Redis rate limiting phân tán khi production chạy nhiều application instance; không thay đổi OTP/Payment.

## 21. Implementation Checklist

Chỉ `[x]` cho thành phần đã tồn tại trong codebase tại ngày phân tích. Các dòng BLOCKED không được triển khai trước khi endpoint thật tồn tại và plan được duyệt.

### Existing baseline

- [x] Redis starter dependency đã có trong `pom.xml`
- [x] `.env` đã được ignore khỏi Git
- [x] Redis configuration/connection properties
- [x] Spring Cache/Redis cache manager
- [x] Serializer strategy
- [ ] Redis Docker configuration được track
- [x] Redis cache unit/service tests

### Phase 1

- [x] Redis architecture approved
- [x] Redis configuration approved
- [x] Common cache names/error handler/kill switch
- [x] `GET /api/modules` read cache pilot
- [x] Existing authorization catalog LIST/DETAIL cache
- [ ] Product List Cache — BLOCKED: API không tồn tại
- [ ] Product Detail Cache — BLOCKED: API không tồn tại
- [ ] Category List Cache — BLOCKED: API không tồn tại
- [ ] Category Detail Cache — BLOCKED: API không tồn tại
- [ ] Brand List Cache — BLOCKED: API không tồn tại
- [ ] Brand Detail Cache — BLOCKED: API không tồn tại
- [ ] Promotion List Cache — BLOCKED: API không tồn tại
- [ ] Promotion Detail Cache — BLOCKED: API không tồn tại
- [x] Cache invalidation cho các cache Phase 1A
- [x] TTL configuration
- [x] Redis failure fallback
- [x] Unit/service tests
- [x] Documentation

### Phase 2

- [x] Effective permission cache per user
- [x] Effective permission cache security invalidation
- [x] User role LIST cache
- [x] User direct-permission LIST cache
- [x] Phase 2 TTL/environment configuration
- [x] Phase 2 cache hit/invalidation/serializer tests
- [ ] Inventory READ Cache — BLOCKED: API không tồn tại
- [ ] Customer READ Cache — BLOCKED: API không tồn tại
- [ ] User READ Cache — BLOCKED: API không tồn tại
- [ ] Order READ Cache — BLOCKED: API không tồn tại
- [ ] Other suitable READ APIs — BLOCKED: API phù hợp khác chưa tồn tại

### Phase 3

- [ ] Inventory atomic operation
- [ ] Stock reservation
- [ ] Distributed Lock
- [ ] Idempotency
- [ ] Cache Stampede protection
- [ ] Rate Limiting bằng Redis (fixed-window in-memory hiện đã có, nhưng chưa distributed)

### Approval gates

- [x] Chủ dự án review file plan này
- [x] Chủ dự án xác nhận rõ `OK, triển khai Phase 1`
- [x] Chủ dự án yêu cầu triển khai `Phase 2`
- [x] Chốt và triển khai Phase 1A cho API hiện có
- [ ] Chốt Phase 1B sau khi commerce API tồn tại
