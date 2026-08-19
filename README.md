# Lunaria Boutique - Fashion E-commerce System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-316192?logo=postgresql)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

Lunaria Boutique là hệ thống thương mại điện tử thời trang đa cửa hàng. Dự án hiện được phát triển theo **kiến trúc monolith**: một ứng dụng backend Spring Boot dùng chung một cơ sở dữ liệu PostgreSQL, kết hợp với một ứng dụng frontend React.

> Dự án không còn sử dụng kiến trúc microservice, API Gateway, Kafka hoặc cơ sở dữ liệu tách riêng theo service.

## Công nghệ sử dụng

### Backend

- Java 21
- Spring Boot 4.1.0
- Spring MVC
- Spring Data JPA / Hibernate
- Spring Security
- Jakarta Validation
- PostgreSQL
- Maven Wrapper

### Frontend

- React 19
- Vite 8
- React Router
- Tailwind CSS 4
- Chart.js / Recharts
- Framer Motion
- Leaflet

## Kiến trúc hiện tại

```text
fashion-system/
|-- backend/
|   `-- fashion-system/        # Ứng dụng Spring Boot monolith
|       |-- src/main/java/     # Entity, DTO, mapper và mã nguồn backend
|       |-- src/main/resources/
|       |-- .env.example       # Mẫu cấu hình PostgreSQL
|       `-- pom.xml
|-- frontend/
|   `-- react-app/             # Ứng dụng React/Vite
|       |-- src/
|       `-- package.json
|-- HUONG_DAN_CHAY_DU_AN.md
`-- README.md
```

Backend gom các miền nghiệp vụ người dùng, phân quyền, sản phẩm, cửa hàng, kho, đơn hàng, thanh toán, khuyến mãi, khách hàng thân thiết, thông báo và chat trong cùng một ứng dụng và một database.

## Các màn hình và nghiệp vụ đang phát triển

- Trang mua sắm dành cho khách hàng: sản phẩm, thương hiệu, bộ sưu tập và khuyến mãi.
- Giỏ hàng, thanh toán, đơn hàng và thông tin khách hàng.
- Khu vực quản trị: sản phẩm, biến thể, danh mục, thương hiệu và bộ sưu tập.
- Quản lý khách hàng, nhân viên, vai trò, nhà cung cấp và công việc.
- Quản lý kho, phiếu nhập, phiếu xuất, đơn hàng và thống kê.
- Giao diện chat và thông báo.
- Giao diện responsive và hỗ trợ light/dark theme.

## Trạng thái dự án

Dự án đang trong quá trình phát triển:

- Frontend đã có các màn hình cho khu vực khách hàng và quản trị; nhiều luồng vẫn sử dụng mock data.
- Backend hiện có mô hình dữ liệu, DTO, mapper và cấu hình kết nối PostgreSQL.
- Phần API, xác thực/phân quyền, thanh toán và kết nối đầy đủ giữa frontend với backend chưa hoàn thiện.
- Schema PostgreSQL hợp nhất nằm tại `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql`.

## Chạy nhanh

Yêu cầu Java 21, Node.js phù hợp với Vite 8 và PostgreSQL. Sau khi tạo database và cấu hình môi trường, chạy backend và frontend ở hai terminal riêng:

```powershell
# Terminal 1
cd backend\fashion-system
.\mvnw.cmd spring-boot:run

# Terminal 2
cd frontend\react-app
npm install
npm run dev
```

Frontend mặc định mở tại `http://localhost:5173`; backend mặc định chạy tại `http://localhost:8080`.

Xem hướng dẫn cài đặt, cấu hình database, lệnh chạy trên từng hệ điều hành và cách xử lý lỗi tại [HUONG_DAN_CHAY_DU_AN.md](./HUONG_DAN_CHAY_DU_AN.md).

## Lưu ý bảo mật

- Không commit file `backend/fashion-system/.env` hoặc mật khẩu database thật.
- Chỉ dùng `.env.example` làm mẫu cấu hình.
- Thay các thông tin mặc định trước khi triển khai lên môi trường thật.

## Mục tiêu

Đây là dự án cá nhân phục vụ việc xây dựng một hệ thống thương mại điện tử đầy đủ theo kiến trúc monolith, tập trung vào mô hình dữ liệu, nghiệp vụ bán hàng đa cửa hàng, quản trị kho, phân quyền và trải nghiệm người dùng.
