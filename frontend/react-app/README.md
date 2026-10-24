# Lunaria – Frontend (React + Vite)

```bash
npm run dev     # chạy dev server (proxy /api, /ws tới BACKEND_URL trong .env)
npm run build   # build production
npm run lint    # ESLint
npm test        # chạy toàn bộ test trong tests/ bằng node:test
```

## Cấu trúc gọi API (`src/api/`)

Mọi lời gọi HTTP tới backend nằm trong `src/api/`. Component, page, hook **không** gọi `fetch` trực tiếp.
Thư mục được chia theo **ai được phép gọi endpoint**:

| Thư mục | Nội dung | Gửi request qua |
| --- | --- | --- |
| `http/` | Tầng dùng chung: `API_BASE_URL`, `queryString`, phiên + hàm request kèm refresh token | — |
| `auth/` | Endpoint `/api/auth` (đăng nhập, đăng ký, OTP) cho cả nhân viên và khách | `fetch` riêng trong `authApi.js` |
| `admin/` | Endpoint cần phiên nhân viên (quản trị) | `requestAdmin` / `requestAdminBlob` |
| `customer/` | Endpoint cần phiên khách hàng đã đăng nhập | `requestCustomer` |
| `storefront/` | Endpoint công khai của cửa hàng online | `requestPublic` |

Quy ước:

- Tên file `<miền>Api.js`, export một object theo miền (`orderApi`, `cartApi`, …) với các hàm `list`, `detail`, `create`, …
- Endpoint mới thuộc miền đã có → thêm vào file đó; miền mới → tạo file mới đúng thư mục theo bảng trên.
- Query string luôn dựng bằng `queryString()` trong `http/query.js`, không tự viết lại.

## Test (`tests/`)

Test nằm trong `tests/`, **mirror đúng đường dẫn** của file được test trong `src/`:
`src/pages/admin/orderManagementLogic.js` → `tests/pages/admin/orderManagementLogic.test.js`.
Test chỉ dành cho logic thuần (file `*Logic.js`, `realtime/`, `api/`), import từ `../../src/...`.
