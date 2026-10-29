# Lunaria Boutique - Fashion E-commerce System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-316192?logo=postgresql)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

Lunaria Boutique là hệ thống thương mại điện tử thời trang **đa cửa hàng**, gồm cửa hàng online cho khách và khu vực quản trị cho nhân viên (bán tại quầy, kho, đơn hàng, khuyến mãi, phân quyền).

Dự án theo **kiến trúc monolith**: một backend Spring Boot dùng chung một database PostgreSQL và một frontend React. Kafka và Redis chạy kèm để xử lý thông báo, audit log và cache, không phải để tách service.

## Tính năng chính

**Khách hàng (storefront)**

- Duyệt sản phẩm, thương hiệu, bộ sưu tập, khuyến mãi; xem tồn kho tại từng cửa hàng và bản đồ cửa hàng.
- Đăng ký / đăng nhập (kể cả Google), quên mật khẩu qua OTP email.
- Giỏ hàng lưu trên server, danh sách yêu thích, sổ địa chỉ.
- Đặt hàng online, thanh toán COD / VNPay / MoMo; đơn có thể được nhiều cửa hàng cùng xử lý.
- Theo dõi đơn, hủy đơn, chat hỗ trợ theo đơn (bot + nhân viên).
- Đổi size/màu hoặc trả hàng trong 14 ngày sau khi đơn hoàn thành: gửi yêu cầu online, mang hàng tới cửa hàng đã bán.
- Gợi ý trên trang sản phẩm: "Thường được mua cùng" (từ các đơn thật) và "Có thể bạn cũng thích" (cùng danh mục / bộ sưu tập / thương hiệu).
- Điểm tích lũy, hạng khách hàng, ví voucher đổi bằng điểm.
- Đánh giá sản phẩm (sao, nhận xét, tối đa 3 ảnh) cho sản phẩm trong đơn đã hoàn thành; hiển thị kèm nhãn "Đã mua hàng".
- Báo khi có hàng: đăng ký theo màu/size đang hết hàng online, nhận thông báo khi được nhập thêm (quét mỗi `STOCK_ALERT_CHECK_MS`, mặc định 2 phút).
- Thông báo realtime.

**Quản trị**

- Danh mục: sản phẩm, biến thể, thuộc tính, danh mục, thương hiệu, bộ sưu tập, tag, nhà cung cấp.
- Kho: tồn kho theo cửa hàng, phiếu nhập, phiếu xuất; điều chuyển hàng giữa cửa hàng (nơi thiếu yêu cầu → nơi dư duyệt & xuất → nơi nhận xác nhận) kèm danh sách hàng sắp hết và gợi ý cửa hàng còn dư.
- Bán hàng: quản lý đơn online/offline, bán tại quầy (POS), giao hàng qua Lalamove, chat hỗ trợ.
- Khách hàng, khuyến mãi, hạng khách hàng.
- Đánh giá sản phẩm: lọc, ẩn có lý do, trả lời khách.
- Đổi trả: duyệt / từ chối, nhận hàng (còn tốt → tồn tại quầy, lỗi → tồn hỏng), đổi phân loại, hoàn tiền mặt tại quầy hoặc xác nhận hoàn qua VNPay/MoMo.
- Nhân sự: nhân viên, phòng ban, vị trí, cửa hàng.
- Phân quyền theo vai trò / module / nhóm quyền, giới hạn theo phạm vi cửa hàng.
- Nhật ký hệ thống (audit log nghiệp vụ và đăng nhập).

## Công nghệ

| | |
| --- | --- |
| **Backend** | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, WebSocket/STOMP, Mail), JWT, PostgreSQL |
| **Hạ tầng phụ** | Kafka (thông báo, audit log), Redis (cache), Cloudinary (ảnh) |
| **Tích hợp** | VNPay, MoMo, Lalamove, Google Sign-In |
| **Frontend** | React 19, Vite 8, React Router 7, Tailwind CSS 4, STOMP.js, Leaflet, Chart.js / Recharts, Framer Motion |

## Cấu trúc thư mục

```text
fashion-system/
|-- backend/fashion-system/        # Spring Boot monolith
|   |-- src/main/java/.../         # controller, service, repository, entity, dto,
|   |                              # security, payment, shipping, notification, realtime, audit
|   |-- src/main/resources/db/
|   |   |-- migration/             # Flyway: V1 schema, V2 dữ liệu danh mục, V3+ thay đổi mới
|   |   |-- archive/               # V22..V41 cũ (đã gộp vào V1/V2), chỉ để tra cứu
|   |   `-- seed/                  # dữ liệu mẫu kho / cửa hàng
|   `-- .env.example               # mẫu cấu hình
|-- frontend/react-app/            # React + Vite (xem README riêng)
|-- docker-compose.audit.yml       # Kafka + Redis cho môi trường local
`-- docs/                          # tài liệu thiết kế
```

Quy ước bên frontend (tổ chức `src/api/`, `tests/`) được mô tả trong [frontend/react-app/README.md](./frontend/react-app/README.md).

## Chạy bằng Docker (toàn bộ hệ thống)

Chỉ cần Docker Desktop. Postgres, Redis, Kafka, backend và frontend chạy trong container; database trống được Flyway dựng tự động.

```powershell
copy .env.docker.example .env    # điền JWT_SECRET và BOOTSTRAP_ADMIN_PASSWORD
docker compose up -d --build
```

- Cửa hàng và trang quản trị: <http://localhost:8081> (đăng nhập quản trị tại `/adminlogin` bằng `superadmin` / `BOOTSTRAP_ADMIN_PASSWORD`).
- Backend và Swagger: <http://localhost:8080/swagger-ui.html>. Postgres mở ở cổng `5433` để không đụng PostgreSQL cài trên máy.
- Database mới chỉ có dữ liệu danh mục (vai trò, quyền, hạng khách hàng); cửa hàng, sản phẩm... tạo qua trang quản trị. Muốn dùng lại dữ liệu của database local thì chép sang **trước khi** backend chạy lần đầu:

  ```powershell
  docker compose up -d --wait postgres
  pg_dump -U postgres --no-owner --no-privileges commerce_db | docker compose exec -T postgres psql -q -U postgres -d commerce_db
  docker compose up -d --build
  ```
- Dừng: `docker compose down` (thêm `-v` để xóa luôn dữ liệu).

Stack này dùng tên project `lunaria` nên chạy song song được với `docker-compose.audit.yml` bên dưới, nhưng cả hai cùng dùng cổng 8080 với backend chạy local.

## Chạy local

Cần có: Java 21, Node.js 20+, PostgreSQL, Docker (để chạy Kafka + Redis).

1. **Database**: tạo một database PostgreSQL trống. Sao chép `backend/fashion-system/.env.example` thành `.env` rồi điền thông tin kết nối, `JWT_SECRET` và `BOOTSTRAP_ADMIN_PASSWORD`. Khi backend khởi động, Flyway tự tạo schema và dữ liệu danh mục (vai trò, quyền, hạng khách hàng...), rồi tạo tài khoản SUPER_ADMIN `BOOTSTRAP_ADMIN_USERNAME` nếu chưa có. Dữ liệu mẫu kho / cửa hàng trong `db/seed/` là tùy chọn, chạy bằng `psql`.

   Database cũ đã chạy tay tới V41 sẽ được Flyway đánh dấu baseline ở V2, không chạy lại gì. Thay đổi schema mới thêm thành file `V3__...sql`, `V4__...sql` trong `db/migration/`; không sửa file đã chạy.
2. **Kafka + Redis**:

   ```powershell
   docker compose -f docker-compose.audit.yml up -d
   ```

   Không có Kafka thì backend vẫn chạy được, nhưng thông báo sẽ không được gửi đi.
3. **Backend và frontend**: chạy ở hai terminal riêng.

   ```powershell
   # Terminal 1 - backend (http://localhost:8080)
   cd backend\fashion-system
   .\mvnw.cmd spring-boot:run

   # Terminal 2 - frontend (http://localhost:5173)
   cd frontend\react-app
   npm install
   npm run dev
   ```

   Muốn gọi qua proxy của Vite thì đặt `BACKEND_URL=http://localhost:8080` trong `frontend/react-app/.env`.

Thanh toán, giao hàng, email và Cloudinary chỉ hoạt động khi đã khai báo key tương ứng trong `.env`.

**Tài liệu API và health check** (backend đang chạy):

- Swagger UI: <http://localhost:8080/swagger-ui.html> khi `API_DOCS_ENABLED=true` (mặc định tắt). API chia ba nhóm: quản trị, storefront/khách hàng, xác thực/webhook. Đăng nhập bằng `POST /api/auth/login/employee` hoặc `/api/auth/login/customer`, rồi dán `token` vào nút **Authorize**.
- Health: <http://localhost:8080/actuator/health> trả `{"status":"UP"}` khi kết nối được database; chỉ endpoint này của Actuator được mở.

## Kiểm thử

```powershell
cd backend\fashion-system; .\mvnw.cmd test   # backend
cd frontend\react-app;     npm test          # frontend (node:test)
```

Test backend chạy trên database trong `.env` và rollback dữ liệu sau mỗi test. Test cần "cửa hàng / sản phẩm đầu tiên" tự tạo dữ liệu tối thiểu khi database trống, nên chạy được cả trên database mới (`DB_NAME=<db trống>`).

GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml)) chạy mỗi lần push: test backend trên PostgreSQL trống, lint + test + build frontend, rồi build hai Docker image.

## Trạng thái

Dự án đang phát triển. Các luồng khách hàng và quản trị, kể cả dashboard và trang thống kê, đã dùng API và dữ liệu thật.

## Lưu ý bảo mật

- Không commit `backend/fashion-system/.env`, `frontend/react-app/.env` hay bất kỳ key thật nào; chỉ commit `.env.example`.
- Đổi toàn bộ secret mặc định (`JWT_SECRET`, mật khẩu database, key thanh toán) trước khi triển khai thật.

## Mục tiêu

Dự án cá nhân, nhằm xây dựng một hệ thống thương mại điện tử đầy đủ theo kiến trúc monolith, tập trung vào nghiệp vụ bán hàng đa cửa hàng, quản trị kho, phân quyền và trải nghiệm người dùng.
