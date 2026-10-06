# Backend Read Cache Design

## Mục tiêu

Rà soát toàn bộ controller, service và repository của backend Fashion System để xác định API đọc nào đang dùng Redis, bổ sung cache cho các API list/detail thuộc catalog và master data phù hợp, đồng thời bảo đảm cache key không va chạm và mutation không để lại dữ liệu stale.

## Phạm vi

- Giữ nguyên Spring Cache và `RedisCacheManager` hiện có.
- Rà toàn bộ API GET đang được controller công khai và các service/repository phục vụ chúng.
- Bổ sung cache cho dữ liệu đọc nhiều, ít thay đổi: catalog sản phẩm và master/reference data.
- Sửa các cache hiện có nếu thiếu invalidation, key không đầy đủ hoặc serializer không hỗ trợ kiểu kết quả.
- Không thay đổi contract HTTP hoặc refactor nghiệp vụ ngoài nhu cầu cache.

## API được cache

Nhóm đã có cache tiếp tục được giữ và kiểm tra lại:

- Module và authorization catalog/role/user assignment/effective permissions.
- Detail của brand, category, collection, product, product variant, product attribute, product tag, promotion, store, department, position, supplier và customer tier.
- Danh sách customer tier có thứ tự.

Nhóm sẽ được xem xét bổ sung sau khi xác nhận serializer và đường invalidation:

- Danh sách phân trang của brand, category, collection, product, product variant, product attribute, product tag, promotion, store, department, position, supplier và customer tier.
- Danh sách/chi tiết ảnh sản phẩm nếu việc cập nhật ảnh có thể invalidate chính xác theo variant.
- Các catalog lookup kho chỉ được cache ở lớp đọc nằm sau bước kiểm tra quyền; không đặt `@Cacheable` trên method mà cache hit có thể bỏ qua authorization.

Chỉ những API có đường mutation/invalidation xác định được mới được thêm cache.

## API không cache

- Inventory balance, inventory transaction và inventory statistics.
- Goods receipt và goods issue.
- Audit log và auth audit log.
- Customer, address, social account, tier assignment và hồ sơ cá nhân.
- Employee administration, user scope và dữ liệu tổ chức phụ thuộc quyền người dùng.
- Order, payment, refund, shipment, reservation và các trạng thái workflow khác.
- Kiểm tra coordinate availability, impact count và dữ liệu biến động/real-time tương tự.

Các nhóm này có tính real-time, nhạy cảm, phụ thuộc user/store hoặc có tần suất mutation cao; cache có rủi ro stale hoặc vượt quyền lớn hơn lợi ích.

## Cache key

Một utility tạo key canonical sẽ mã hóa từng thành phần theo dạng không nhập nhằng. Key list phải bao gồm:

- Mọi filter sau khi được service nhận vào, kể cả `null`, chuỗi rỗng, enum/status và UUID.
- `page`, `size` và toàn bộ sort order gồm property, direction, ignore-case và null handling.
- `storeId`, `userId` hoặc scope/visibility khi kết quả thực sự phụ thuộc các giá trị đó.

Key detail dùng định danh entity và thêm parent ID khi endpoint detail thuộc aggregate lồng nhau. Không ghép chuỗi bằng delimiter đơn giản vì dữ liệu filter có thể chứa delimiter và gây collision.

## Invalidation

- Create: xóa toàn bộ list cache của aggregate.
- Update: cập nhật hoặc xóa detail cache tương ứng và xóa toàn bộ list cache của aggregate.
- Delete/deactivate/restore/activate: xóa detail cache tương ứng và toàn bộ list cache của aggregate.
- Mutation quan hệ hoặc media: xóa cache của aggregate cha, detail/list phụ thuộc và warehouse lookup liên quan nếu lookup được cache.
- Mutation brand/category/collection ảnh hưởng product hoặc variant phải xóa cả cache product/variant liên quan theo cách an toàn; ưu tiên `allEntries` khi không thể xác định đầy đủ tập key.
- Invalidation chạy qua Spring Cache transaction-aware để chỉ được áp dụng sau commit thành công.

## Serialization và lỗi Redis

- Kiểm chứng bằng test rằng giá trị list/page thực tế có thể round-trip qua serializer Redis hiện tại.
- Chỉ whitelist các kiểu hạ tầng tối thiểu cần thiết; không mở rộng polymorphic typing tùy ý.
- Không cache null.
- Giữ `ResilientCacheErrorHandler`: Redis lỗi không được làm hỏng luồng đọc/ghi database; cảnh báo được rate-limit theo cấu hình hiện tại.
- Cache name mới phải được khai báo tập trung trong `CacheNames` và đăng ký trong `RedisCacheConfig`, vì cache manager đang tắt tạo cache động.

## Kiểm thử

- Test cache key phân biệt filter, page, size, sort và các scope liên quan.
- Test cache hit chỉ gọi repository một lần.
- Test mỗi mutation xóa/cập nhật đúng detail và list cache.
- Test cross-aggregate invalidation cho catalog và media.
- Test serializer round-trip cho DTO list/page được cache.
- Test Redis failure fallback tiếp tục hoạt động.
- Chạy test mục tiêu, toàn bộ `mvn test`, rồi `mvn -DskipTests clean compile`.

## Tiêu chí hoàn tất

- Có danh mục đầy đủ API đã cache, API mới được cache và API chủ động không cache.
- Không có cache key thiếu filter/page/sort/scope.
- Không có cache hit bỏ qua authorization bắt buộc.
- Mutation liên quan không để lại cache stale theo các test đã thêm.
- Test và compile backend hoàn tất; mọi lỗi còn lại được nêu rõ.
