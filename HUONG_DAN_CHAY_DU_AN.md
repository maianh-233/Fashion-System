# Hướng dẫn chạy dự án Lunaria Boutique

Tài liệu này hướng dẫn chạy dự án ở môi trường local. Hệ thống gồm ba thành phần:

- PostgreSQL: lưu trữ dữ liệu dùng chung.
- Backend: một ứng dụng Spring Boot monolith.
- Frontend: một ứng dụng React chạy bằng Vite.

## 1. Yêu cầu môi trường

Cài đặt các công cụ sau:

| Công cụ | Phiên bản |
| --- | --- |
| Java (JDK) | 21 |
| Node.js | `^20.19.0` hoặc `>=22.12.0` |
| npm | Đi kèm Node.js |
| PostgreSQL | Khuyến nghị 15 trở lên |

Không bắt buộc cài Maven toàn cục vì backend đã có Maven Wrapper (`mvnw` và `mvnw.cmd`).

Kiểm tra môi trường:

```powershell
java -version
node --version
cmd /c npm --version
psql --version
```

Trên macOS/Linux có thể dùng trực tiếp `npm --version` thay cho `cmd /c npm --version`.

## 2. Tạo và khởi tạo PostgreSQL

### Cách 1: Dùng dòng lệnh `psql`

Từ thư mục gốc của dự án, đăng nhập PostgreSQL:

```powershell
psql -U postgres
```

Tạo database trong cửa sổ `psql`:

```sql
CREATE DATABASE commerce_db;
\q
```

Nạp schema hợp nhất:

```powershell
psql -U postgres -d commerce_db -f ".\backend\fashion-system\src\main\java\com\fashionsystem\fashion_system\db\db.sql"
```

Trên macOS/Linux:

```bash
psql -U postgres -d commerce_db -f ./backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql
```

Script sử dụng extension `pgcrypto`. Tài khoản chạy script cần có quyền tạo extension trong database.

### Cách 2: Dùng pgAdmin

1. Tạo database tên `commerce_db`.
2. Mở Query Tool của database vừa tạo.
3. Mở file `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql`.
4. Chạy toàn bộ script.

Schema chỉ cần nạp lần đầu. File SQL hiện không phải migration lặp lại; chạy lại trên database đã có bảng có thể báo lỗi `already exists`.

## 3. Cấu hình backend

Đi vào thư mục backend và tạo `.env` từ file mẫu.

PowerShell:

```powershell
cd backend\fashion-system
Copy-Item .env.example .env
```

Command Prompt:

```bat
cd backend\fashion-system
copy .env.example .env
```

macOS/Linux:

```bash
cd backend/fashion-system
cp .env.example .env
```

Mở `.env` và sửa thông tin cho đúng với PostgreSQL trên máy:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=commerce_db
DB_USERNAME=postgres
DB_PASSWORD=your_password

JPA_DDL_AUTO=validate
SHOW_SQL=false
```

Ý nghĩa các biến:

| Biến | Mô tả |
| --- | --- |
| `DB_HOST` | Máy chủ PostgreSQL |
| `DB_PORT` | Cổng PostgreSQL, mặc định là `5432` |
| `DB_NAME` | Tên database |
| `DB_USERNAME` | Tài khoản kết nối database |
| `DB_PASSWORD` | Mật khẩu kết nối database |
| `JPA_DDL_AUTO` | Chế độ xử lý schema của Hibernate; nên giữ `validate` |
| `SHOW_SQL` | Bật/tắt log câu lệnh SQL |

File `.env` đã được backend bỏ qua trong Git. Không đưa mật khẩu thật vào `.env.example`.

## 4. Chạy backend

Phải chạy lệnh trong thư mục `backend/fashion-system` để Spring Boot đọc đúng file `.env`.

PowerShell hoặc Command Prompt:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Khi khởi động thành công, backend mặc định chạy tại:

```text
http://localhost:8080
```

Lưu ý: backend chưa có API/controller hoàn chỉnh. Truy cập trực tiếp địa chỉ trên có thể không hiển thị trang hoặc có thể được Spring Security yêu cầu xác thực; điều này không đồng nghĩa ứng dụng khởi động thất bại. Hãy kiểm tra terminal có dòng báo ứng dụng đã started và không có exception.

## 5. Chạy frontend

Mở terminal mới từ thư mục gốc dự án:

```powershell
cd frontend\react-app
cmd /c npm install
cmd /c npm run dev
```

Nếu PowerShell trên máy cho phép chạy script npm, có thể dùng `npm install` và `npm run dev` trực tiếp.

Trên macOS/Linux:

```bash
cd frontend/react-app
npm install
npm run dev
```

Mở địa chỉ Vite in ra terminal, mặc định là:

```text
http://localhost:5173
```

Một số đường dẫn để kiểm tra giao diện:

- `/`: trang chủ khách hàng.
- `/products`: danh sách sản phẩm.
- `/adminlogin`: đăng nhập quản trị.
- `/admin`: dashboard quản trị.

Frontend hiện còn sử dụng mock data ở nhiều màn hình nên có thể chạy và xem giao diện dù chưa kết nối API backend.

## 6. Build và kiểm tra

### Backend

PowerShell/Windows:

```powershell
cd backend\fashion-system
.\mvnw.cmd test
.\mvnw.cmd clean package
```

macOS/Linux:

```bash
cd backend/fashion-system
./mvnw test
./mvnw clean package
```

File JAR được tạo trong `backend/fashion-system/target/`.

### Frontend

```powershell
cd frontend\react-app
cmd /c npm run lint
cmd /c npm run build
```

Build frontend được tạo trong `frontend/react-app/dist/`.

## 7. Thứ tự chạy đề xuất

1. Khởi động PostgreSQL.
2. Tạo `commerce_db` và nạp `db.sql` nếu đây là lần chạy đầu tiên.
3. Cấu hình `backend/fashion-system/.env`.
4. Chạy backend tại cổng `8080`.
5. Chạy frontend tại cổng `5173`.

## 8. Lỗi thường gặp

### `Connection refused` hoặc không kết nối được PostgreSQL

- Kiểm tra dịch vụ PostgreSQL đang chạy.
- Kiểm tra `DB_HOST`, `DB_PORT`, tên database, username và password trong `.env`.
- Thử đăng nhập bằng chính thông tin đó qua `psql` hoặc pgAdmin.

### `Schema-validation: missing table`

`JPA_DDL_AUTO=validate` yêu cầu schema phải tồn tại trước khi backend chạy. Hãy nạp file `db.sql` vào đúng database.

### Cổng `8080` hoặc `5173` đã được sử dụng

- Dừng tiến trình đang chiếm cổng; hoặc
- Đổi cổng backend bằng biến môi trường `SERVER_PORT`; hoặc
- Chạy frontend bằng `npm run dev -- --port 5174`.

### PowerShell báo `npm.ps1 cannot be loaded`

Dùng phiên bản lệnh qua Command Prompt ngay trong PowerShell:

```powershell
cmd /c npm install
cmd /c npm run dev
```

### Maven tải dependency thất bại

Kiểm tra kết nối mạng, proxy và Java đang dùng đúng phiên bản 21, sau đó chạy lại Maven Wrapper.

## 9. Dừng dự án

Nhấn `Ctrl + C` trong terminal backend và terminal frontend. PostgreSQL có thể được giữ chạy cho lần phát triển tiếp theo.
