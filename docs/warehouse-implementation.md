# Kho theo cửa hàng, phiếu nhập và phiếu xuất

## Kiến trúc và domain sau thay đổi

Backend Spring Boot/JPA/PostgreSQL, frontend React/Vite. Tái sử dụng JWT và RBAC hiện có.

| Domain | Entity thực tế và quan hệ |
|---|---|
| Store | Một cửa hàng có nhiều InventoryBalance, GoodsReceipt và GoodsIssue |
| Employee | User + StoreStaff đang hoạt động; không tạo hệ thống employee/scope thứ hai |
| ProductVariant | Thuộc Product; tồn kho không đặt trên Product |
| Inventory | InventoryBalance có khóa chính kép storeId + productVariantId |
| Supplier | Một nhà cung cấp cho mỗi phiếu nhập; bắt buộc khi xuất trả nhà cung cấp |
| ImportReceipt / Item | GoodsReceipt 1–N GoodsReceiptItem; mỗi item tham chiếu Product + ProductVariant, targetChannel |
| ExportReceipt / Item | GoodsIssue 1–N GoodsIssueItem; sourceChannel và loại xuất ở header |
| StockTransaction | InventoryTransaction tham chiếu cửa hàng, variant, phiếu nhập/xuất và nhân viên thực hiện |

Quan hệ dùng UUID theo convention của project, có FK database. Không sửa cấu trúc Product, ProductVariant, User, StoreStaff, Store hay Supplier.

## Entity và dữ liệu

- InventoryBalance thêm onlineQuantity, version; offlineQuantity ánh xạ vào cột available_quantity cũ. Tổng tính từ offline + online, không lưu trùng. Giữ reserved/damaged cũ để không mất dữ liệu; giữ chỗ cũ vẫn dùng bucket offline.
- Hai loại phiếu thêm confirmedAt, completedAt, completedBy. receivedBy/issuedBy là người tạo, approvedBy là người xác nhận. Mã IMP/EXP + ngày + UUID do server sinh, cột unique và không cập nhật.
- GoodsIssue thêm supplierId, reason; giữ orderId cũ ở dữ liệu lịch sử nhưng loại khỏi request mới.
- Hai loại item thêm productId và channel. Backend xác minh variant thuộc product, quantity dương, giới hạn integer và tổng tiền.
- InventoryTransaction thêm snapshot trước/sau online/offline, fromChannel/toChannel, importReceiptId/exportReceiptId. Giao dịch cũ giữ snapshot NULL khi không thể suy ra đáng tin cậy.
- Không xóa field lịch sử; loại bỏ khả năng ghi các field audit/status/code từ API request bằng DTO riêng.

## Nghiệp vụ và backend

GoodsReceiptService và GoodsIssueService dùng DRAFT → PENDING_CONFIRMATION → CONFIRMED → COMPLETED. DRAFT/PENDING được sửa hoặc hủy. Sau xác nhận không sửa/hủy. Create/confirm không ghi tồn; complete khóa phiếu, khóa tồn theo thứ tự variant, ghi lịch sử và audit trong cùng transaction. Complete lặp lại bị từ chối.

InventoryService dùng upsert an toàn rồi PESSIMISTIC_WRITE. Kiểm tra tồn bucket, không âm và không tràn số trước khi cập nhật. Hai loại chuyển kênh giữ nguyên tổng; DAMAGED/RETURN_TO_SUPPLIER/OTHER giảm tổng.

StoreAccessService kiểm tra permission action + effective permission scope + employee scope tại service. Store employee không được vượt cửa hàng được gán kể cả có grant ALL. Global employee cần grant ALL cho action; grant STORE không tự biến thành quyền toàn chuỗi. Danh sách cửa hàng chỉ gồm cửa hàng hoạt động được truy cập.

Tách GoodsReceiptRequest/GoodsReceiptItemRequest và GoodsIssueRequest/GoodsIssueItemRequest khỏi response DTO. Các controller cũ được cập nhật đồng bộ, giữ base URL `/api/import-receipts`, `/api/export-receipts`; danh sách và header nhận storeId, các tài nguyên được kiểm tra cửa hàng đã lưu. Thêm `/submit`, `/confirm`, `/complete`, `/cancel`; `/approve` chỉ xác nhận, DELETE chuyển hủy và giữ lịch sử.

InventoryController bổ sung lọc sản phẩm/SKU/màu/size/danh mục/tình trạng tồn. WarehouseLookupController/Service cung cấp danh sách cửa hàng và catalog tối thiểu cho người vận hành kho, không yêu cầu cấp quyền chỉnh sửa catalog.

Quyền mới: IMPORT_RECEIPT_CONFIRM/COMPLETE/CANCEL và EXPORT_RECEIPT_CONFIRM/COMPLETE/CANCEL. VIEW/CREATE/UPDATE tiếp tục dùng RBAC cũ. Submit dùng UPDATE.

## Migration

`backend/fashion-system/src/main/resources/db/migration/V22__store_warehouse_workflow.sql` chạy tường minh qua psql; project chưa có Flyway tự chạy.

Migration thêm cột, FK, composite FK product+variant, index theo store/status và lịch sử, constraint trạng thái/kênh/số lượng/giá/tồn không âm. PK inventory và unique mã phiếu hiện có được tái sử dụng. APPROVED cũ chuyển COMPLETED mà không ghi tồn lần nữa; PENDING chuyển DRAFT. Loại xuất cũ được giữ trong reason. Một số CHECK dùng NOT VALID để giữ ngoại lệ lịch sử nhưng chặn dữ liệu mới không hợp lệ.

Grant APPROVE/DELETE được chuyển sang CONFIRM/CANCEL và giữ ALLOW/DENY cùng scope. COMPLETE chỉ tự cấp cho role ADMIN hiện có; role kho khác cần quản trị viên cấp rõ ràng.

Xem [hướng dẫn migration](warehouse-migration.md). Chưa áp dụng migration vào database ứng dụng đang dùng; chỉ kiểm chứng trên PostgreSQL tạm. Các file SQL đã bị xóa từ trước trong working tree không được khôi phục hay thay đổi.

## Frontend

InventoryManagement, ImportReceiptManagement, ExportReceiptManagement dùng chung WarehouseManagement. Route hiện có được giữ; storeId/tab trong URL chọn workspace. Nhân viên cửa hàng được cố định vào store của mình. Không có màn hình gộp tồn nhiều cửa hàng.

WarehouseReceiptEditor có chọn supplier → product → variant, nhiều dòng, kênh, giá nhập; form xuất theo type và hiển thị tồn bucket. Action dựa trên status và permission, form readonly sau xác nhận. warehouseLogic kiểm tra số lượng cộng dồn nhiều dòng cùng variant/channel và giới hạn số. Bảng tồn, phiếu và lịch sử có thông tin audit. Khi chưa có tên nhân viên trong DTO, UI hiển thị UUID audit.

API dùng requestAdmin hiện có; frontend không gọi luồng approve làm thay đổi tồn cũ. Không đổi authentication/JWT.

## Kiểm chứng

- Bộ test phiếu và quyền: 50 test qua, bao gồm 4 test PostgreSQL về rollback và hoàn tất đồng thời.
- Migration: PostgreSQL 18 kiểm chứng nâng cấp/chạy lại, bảo toàn dữ liệu, toàn bộ trạng thái, constraint, rollback lỗi và cạnh tranh tồn.
- Frontend: production build, 5 logic test và ESLint cho file thay đổi đều qua; còn cảnh báo kích thước bundle sẵn có. Project dùng JavaScript, không có bước TypeScript riêng.
- Theo yêu cầu tiết kiệm quota, không mở rộng toàn bộ regression suite hoặc browser E2E. Kết quả lượt kiểm tra tồn kho cuối được ghi trong thông báo bàn giao.
