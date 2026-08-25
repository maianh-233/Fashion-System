# Tài liệu hệ thống phân quyền

Tài liệu này mô tả cấu trúc dữ liệu, luồng xác định quyền hiệu lực và các API phân quyền hiện có trong backend Fashion System.

## 1. Tổng quan kiến trúc

```text
Module
  └── PermissionGroup
        └── Permission

User ── UserRole ── Role ── RolePermission ── Permission
User ── UserPermission ─────────────────────── Permission
```

- `Module`, `PermissionGroup`, `Permission` tạo thành catalog quyền nghiệp vụ.
- `RolePermission` cấp permission cho role theo một phạm vi dữ liệu.
- `UserRole` gán một hoặc nhiều role cho user.
- `UserPermission` là ngoại lệ trực tiếp trên user, có thể cấp thêm hoặc từ chối permission.
- Frontend dùng quyền hiệu lực để render module, menu và button.
- Việc ẩn UI không phải là security. Backend vẫn phải kiểm tra permission bằng Spring Security hoặc `AuthorizationService`.

## 2. Ý nghĩa các bảng

### 2.1. `modules`

Đại diện cho một phân hệ nghiệp vụ lớn, ví dụ `HUMAN_RESOURCE`, `CRM`, `REPORTING`.

| Cột | Kiểu/ý nghĩa |
|---|---|
| `id` | UUID, khóa chính. |
| `code` | Mã nghiệp vụ duy nhất, tối đa 50 ký tự. Nên dùng chữ hoa và dấu gạch dưới. |
| `name` | Tên hiển thị của module. |
| `description` | Mô tả module. |
| `icon` | Tên icon để frontend render. |
| `sort_order` | Thứ tự hiển thị module tăng dần. |
| `active` | `false` sẽ loại toàn bộ permission thuộc module khỏi quyền hiệu lực. |
| `created_at` | Thời điểm tạo. |

Quan hệ: một module có nhiều permission group.

### 2.2. `permission_groups`

Nhóm các permission cùng một chức năng/menu, ví dụ `EMPLOYEE`, `PAYROLL`.

| Cột | Kiểu/ý nghĩa |
|---|---|
| `id` | UUID, khóa chính. |
| `module_id` | Module sở hữu group, bắt buộc. |
| `code` | Mã group duy nhất. |
| `name` | Tên hiển thị. |
| `description` | Mô tả nhóm chức năng. |
| `created_at` | Thời điểm tạo. |

Quan hệ: một permission group thuộc đúng một module và có nhiều permission.

### 2.3. `permissions`

Đại diện cho một hành động thực tế mà backend có thể kiểm tra. Permission không bị giới hạn ở CRUD.

Ví dụ:

```text
EMPLOYEE_VIEW
EMPLOYEE_CREATE
PAYROLL_CALCULATE
PAYROLL_APPROVE
DEADLINE_SUBMIT
```

| Cột | Kiểu/ý nghĩa |
|---|---|
| `id` | UUID, khóa chính. |
| `group_id` | Permission group sở hữu permission, bắt buộc. |
| `code` | Mã permission duy nhất, dùng làm Spring Security authority. |
| `name` | Tên hành động hiển thị. |
| `description` | Mô tả chi tiết. |
| `created_at` | Thời điểm tạo. |

### 2.4. `roles`

Role là tập hợp permission có thể tái sử dụng cho nhiều user, ví dụ một role nghiệp vụ do quản trị viên cấu hình.

| Cột | Kiểu/ý nghĩa |
|---|---|
| `id` | UUID, khóa chính. |
| `code` | Mã role duy nhất. |
| `name` | Tên hiển thị. |
| `description` | Mô tả trách nhiệm của role. |
| `created_at` | Thời điểm tạo. |

Backend không kiểm tra nghiệp vụ bằng tên role. Role chỉ là nguồn cấp permission.

### 2.5. `role_permissions`

Bảng liên kết permission với role.

| Cột | Kiểu/ý nghĩa |
|---|---|
| `role_id` | UUID role, một phần khóa chính. |
| `permission_id` | UUID permission, một phần khóa chính. |
| `scope` | Phạm vi dữ liệu role được phép thao tác. |

Khóa chính kép là `(role_id, permission_id)`, vì vậy một role chỉ có một cấu hình scope cho mỗi permission.

### 2.6. `user_roles`

Bảng gán role cho user nội bộ.

| Cột | Kiểu/ý nghĩa |
|---|---|
| `user_id` | UUID user, một phần khóa chính. |
| `role_id` | UUID role, một phần khóa chính. |
| `assigned_at` | Thời điểm gán. |

Một user có thể có nhiều role. Permission từ tất cả role sẽ được hợp nhất.

### 2.7. `user_permissions`

Override permission trực tiếp cho một user.

| Cột | Kiểu/ý nghĩa |
|---|---|
| `user_id` | UUID user, một phần khóa chính. |
| `permission_id` | UUID permission, một phần khóa chính. |
| `effect` | `ALLOW` hoặc `DENY`. |
| `scope` | Scope của override. Với `DENY`, quyền bị loại hoàn toàn. |

Khóa chính kép là `(user_id, permission_id)`, nên một user chỉ có một override cho mỗi permission.

## 3. Scope dữ liệu

Scope được sắp từ hẹp đến rộng:

```text
SELF < TEAM < DEPARTMENT < ALL
```

| Scope | Ý nghĩa dự kiến |
|---|---|
| `SELF` | Chỉ dữ liệu thuộc chính user. |
| `TEAM` | Dữ liệu của team do user phụ trách/tham gia. |
| `DEPARTMENT` | Dữ liệu thuộc phòng ban của user. |
| `ALL` | Toàn bộ dữ liệu mà module quản lý. |

Nếu nhiều role cùng cấp một permission, hệ thống lấy scope rộng nhất. Ví dụ `SELF + TEAM + DEPARTMENT` cho kết quả `DEPARTMENT`.

## 4. Cách tính quyền hiệu lực

Với mỗi user, backend thực hiện:

1. Lấy permission từ tất cả role của user.
2. Nếu nhiều role cấp cùng permission, giữ scope lớn nhất.
3. Áp dụng `UserPermission`.
4. `DENY` loại permission, kể cả khi role đã cấp.
5. `ALLOW` cấp permission hoặc ghi đè scope lấy từ role.
6. Loại permission thuộc module có `active=false`.
7. Nhóm kết quả theo `Module → PermissionGroup → Permission`.

Thứ tự ưu tiên:

```text
User DENY > User ALLOW > Role Permission
```

Ví dụ:

```text
Role A: EMPLOYEE_VIEW / SELF
Role B: EMPLOYEE_VIEW / TEAM
User override: EMPLOYEE_VIEW / ALLOW / DEPARTMENT

Kết quả: EMPLOYEE_VIEW / DEPARTMENT
```

Nếu override là `DENY`, kết quả không còn `EMPLOYEE_VIEW`.

## 5. Sử dụng trong Spring Security

Kiểm tra permission không cần scope:

```java
@PreAuthorize("hasAuthority('PRODUCT_CREATE')")
```

Kiểm tra permission kèm scope:

```java
@PreAuthorize("""
    @authorizationService.hasPermission(
        authentication,
        'EMPLOYEE_VIEW',
        T(com.fashionsystem.fashion_system.entity.PermissionScope).TEAM
    )
    """)
```

Các API quản trị yêu cầu permission được cấu hình tại:

```yaml
authorization:
  management:
    permission-code: AUTHORIZATION_MANAGE
```

Có thể đổi bằng biến môi trường `AUTHORIZATION_MANAGEMENT_PERMISSION_CODE` mà không sửa controller.

## 6. Quy ước gọi API

Các API bên dưới yêu cầu JWT:

```http
Authorization: Bearer <access-token>
Content-Type: application/json
```

Status thường dùng:

| Status | Ý nghĩa |
|---|---|
| `200` | Đọc/cập nhật thành công. |
| `201` | Tạo mới thành công. |
| `204` | Xóa thành công, không có response body. |
| `400` | Request/enum/quan hệ không hợp lệ hoặc danh sách replace có ID trùng. |
| `401` | Chưa đăng nhập hoặc JWT không hợp lệ. |
| `403` | Không có permission quản trị. |
| `404` | Entity hoặc assignment không tồn tại. |
| `409` | Trùng code hoặc không thể xóa vì còn relation. |

## 7. API quyền của user đang đăng nhập

### 7.1. Cây quyền dùng cho frontend

```http
GET /api/me/permissions
```

Không nhận `userId`; backend lấy user từ Spring Security.

Response mẫu:

```json
{
  "modules": [
    {
      "code": "HUMAN_RESOURCE",
      "name": "Nhân sự",
      "icon": "users",
      "groups": [
        {
          "code": "EMPLOYEE",
          "name": "Nhân viên",
          "permissions": [
            {
              "code": "EMPLOYEE_VIEW",
              "name": "Xem nhân viên",
              "scope": "TEAM"
            }
          ]
        }
      ]
    }
  ]
}
```

User không có quyền sẽ nhận:

```json
{ "modules": [] }
```

### 7.2. Danh sách quyền hiệu lực dạng phẳng

```http
GET /api/authorization/me/permissions
```

Response mẫu:

```json
[
  {
    "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
    "permissionCode": "EMPLOYEE_VIEW",
    "permissionName": "Xem nhân viên",
    "groupCode": "EMPLOYEE",
    "groupName": "Nhân viên",
    "moduleCode": "HUMAN_RESOURCE",
    "moduleName": "Nhân sự",
    "moduleDescription": "Quản trị nhân sự",
    "moduleIcon": "users",
    "moduleSortOrder": 10,
    "scope": "TEAM",
    "source": "ROLE"
  }
]
```

`source` là `ROLE` hoặc `USER` sau khi áp dụng override.

### 7.3. Kiểm tra một permission và scope

```http
GET /api/authorization/me/check?permissionCode=EMPLOYEE_VIEW&scope=TEAM
```

Response:

```json
{
  "permissionCode": "EMPLOYEE_VIEW",
  "requiredScope": "TEAM",
  "allowed": true
}
```

## 8. API quản trị catalog quyền

Base URL:

```text
/api/admin/authorization
```

### 8.1. Module

| Method | API | Chức năng |
|---|---|---|
| `GET` | `/modules` | Danh sách module. |
| `GET` | `/modules/{id}` | Chi tiết module. |
| `POST` | `/modules` | Tạo module. |
| `PUT` | `/modules/{id}` | Cập nhật module. |
| `DELETE` | `/modules/{id}` | Xóa module nếu không còn group. |

Request tạo/cập nhật:

```json
{
  "code": "HUMAN_RESOURCE",
  "name": "Nhân sự",
  "description": "Quản trị nhân sự",
  "icon": "users",
  "sortOrder": 10,
  "active": true
}
```

Response:

```json
{
  "id": "f411cc2b-aa04-4aac-b3ae-356cd5348064",
  "code": "HUMAN_RESOURCE",
  "name": "Nhân sự",
  "description": "Quản trị nhân sự",
  "icon": "users",
  "sortOrder": 10,
  "active": true,
  "createdAt": "2026-08-25T20:30:00"
}
```

### 8.2. Permission group

| Method | API | Chức năng |
|---|---|---|
| `GET` | `/groups` | Danh sách group. |
| `GET` | `/groups/{id}` | Chi tiết group. |
| `POST` | `/groups` | Tạo group. |
| `PUT` | `/groups/{id}` | Cập nhật hoặc chuyển group sang module hợp lệ. |
| `DELETE` | `/groups/{id}` | Xóa nếu group không còn permission. |

Request:

```json
{
  "moduleId": "f411cc2b-aa04-4aac-b3ae-356cd5348064",
  "code": "EMPLOYEE",
  "name": "Nhân viên",
  "description": "Quản lý hồ sơ nhân viên"
}
```

Response bổ sung `id` và `createdAt`.

### 8.3. Permission

| Method | API | Chức năng |
|---|---|---|
| `GET` | `/permissions` | Danh sách permission. |
| `GET` | `/permissions/{id}` | Chi tiết permission. |
| `POST` | `/permissions` | Tạo permission. |
| `PUT` | `/permissions/{id}` | Cập nhật hoặc chuyển permission sang group hợp lệ. |
| `DELETE` | `/permissions/{id}` | Xóa nếu chưa được gán cho role/user. |

Request:

```json
{
  "groupId": "b4e82b85-8405-483f-995f-521522bad742",
  "code": "EMPLOYEE_VIEW",
  "name": "Xem nhân viên",
  "description": "Xem hồ sơ nhân viên theo scope"
}
```

### 8.4. Cây catalog dành cho màn hình chọn quyền

```http
GET /api/admin/authorization/catalog
```

Response:

```json
{
  "modules": [
    {
      "id": "f411cc2b-aa04-4aac-b3ae-356cd5348064",
      "code": "HUMAN_RESOURCE",
      "name": "Nhân sự",
      "icon": "users",
      "sortOrder": 10,
      "active": true,
      "groups": [
        {
          "id": "b4e82b85-8405-483f-995f-521522bad742",
          "code": "EMPLOYEE",
          "name": "Nhân viên",
          "permissions": [
            {
              "id": "7d97e4ca-e4f0-4529-8096-255a408434c7",
              "code": "EMPLOYEE_VIEW",
              "name": "Xem nhân viên"
            }
          ]
        }
      ]
    }
  ]
}
```

Catalog quản trị có thể chứa module inactive để quản trị viên chỉnh sửa. API `/api/me/permissions` mới loại module inactive.

## 9. API quản trị role

### 9.1. CRUD role

| Method | API | Chức năng |
|---|---|---|
| `GET` | `/roles` | Danh sách role. |
| `GET` | `/roles/{roleId}` | Chi tiết role kèm permission. |
| `POST` | `/roles` | Tạo role. |
| `PUT` | `/roles/{roleId}` | Cập nhật role. |
| `DELETE` | `/roles/{roleId}` | Xóa nếu role chưa gán cho user và chưa có permission. |

Request tạo/cập nhật:

```json
{
  "code": "HR_MANAGER",
  "name": "Quản lý nhân sự",
  "description": "Quản lý nghiệp vụ nhân sự"
}
```

Chi tiết role:

```json
{
  "role": {
    "id": "79582f0e-c635-4ea3-b0a8-eb2e9a05d5e1",
    "code": "HR_MANAGER",
    "name": "Quản lý nhân sự",
    "description": "Quản lý nghiệp vụ nhân sự",
    "createdAt": "2026-08-25T20:30:00"
  },
  "permissions": [
    {
      "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
      "code": "EMPLOYEE_VIEW",
      "name": "Xem nhân viên",
      "scope": "DEPARTMENT",
      "groupCode": "EMPLOYEE",
      "moduleCode": "HUMAN_RESOURCE"
    }
  ]
}
```

### 9.2. Gán hoặc cập nhật một permission cho role

```http
POST /api/admin/authorization/roles/{roleId}/permissions
```

```json
{
  "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
  "scope": "DEPARTMENT"
}
```

Response là chi tiết role như mục trên.

### 9.3. Replace toàn bộ permission của role

```http
PUT /api/admin/authorization/roles/{roleId}/permissions
```

```json
[
  {
    "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
    "scope": "DEPARTMENT"
  },
  {
    "permissionId": "ec04997b-5427-4b52-b72a-5549513636c4",
    "scope": "ALL"
  }
]
```

Gửi `[]` để xóa toàn bộ permission của role. Backend kiểm tra toàn bộ permission ID và ID trùng trước khi thay dữ liệu.

### 9.4. Xóa một permission khỏi role

```http
DELETE /api/admin/authorization/roles/{roleId}/permissions/{permissionId}
```

Thành công trả `204 No Content`.

## 10. API gán role cho user

### 10.1. Lấy role của user

```http
GET /api/admin/authorization/users/{userId}/roles
```

```json
[
  {
    "userId": "cd486187-94a2-4881-a905-655283d566a0",
    "roleId": "79582f0e-c635-4ea3-b0a8-eb2e9a05d5e1",
    "assignedAt": "2026-08-25T20:30:00"
  }
]
```

### 10.2. Gán một role

```http
POST /api/admin/authorization/users/{userId}/roles/{roleId}
```

Không có request body. Response là toàn bộ danh sách role hiện tại của user.

### 10.3. Replace toàn bộ role

```http
PUT /api/admin/authorization/users/{userId}/roles
```

```json
{
  "roleIds": [
    "79582f0e-c635-4ea3-b0a8-eb2e9a05d5e1",
    "76a56d29-2904-4c84-bcf1-c48dd2436602"
  ]
}
```

Gửi `roleIds: []` để xóa toàn bộ role của user. User không có role sẽ không nhận permission từ RBAC.

### 10.4. Xóa một role

```http
DELETE /api/admin/authorization/users/{userId}/roles/{roleId}
```

Thành công trả `204 No Content`.

## 11. API override permission trực tiếp cho user

### 11.1. Lấy override hiện có

```http
GET /api/admin/authorization/users/{userId}/permissions
```

```json
[
  {
    "userId": "cd486187-94a2-4881-a905-655283d566a0",
    "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
    "effect": "DENY",
    "scope": "ALL"
  }
]
```

### 11.2. Gán/cập nhật một override

```http
POST /api/admin/authorization/users/{userId}/permissions
```

```json
{
  "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
  "effect": "ALLOW",
  "scope": "TEAM"
}
```

Response là toàn bộ override hiện tại của user.

### 11.3. Replace toàn bộ override

```http
PUT /api/admin/authorization/users/{userId}/permissions
```

```json
[
  {
    "permissionId": "7d97e4ca-e4f0-4529-8096-255a408434c7",
    "effect": "DENY",
    "scope": "ALL"
  },
  {
    "permissionId": "ec04997b-5427-4b52-b72a-5549513636c4",
    "effect": "ALLOW",
    "scope": "DEPARTMENT"
  }
]
```

Gửi `[]` để xóa toàn bộ override. Danh sách có `permissionId` trùng sẽ bị từ chối trước khi dữ liệu cũ bị xóa.

### 11.4. Xóa một override

```http
DELETE /api/admin/authorization/users/{userId}/permissions/{permissionId}
```

Thành công trả `204 No Content`.

## 12. Quy tắc quản trị quan trọng

- `code` của module, group, permission và role là duy nhất; backend chuẩn hóa thành chữ hoa.
- Không thể tạo/chuyển group đến module không tồn tại.
- Không thể tạo/chuyển permission đến group không tồn tại.
- Không thể gán role/permission/user không tồn tại.
- Không thể xóa module còn group.
- Không thể xóa group còn permission.
- Không thể xóa permission đang được role hoặc user sử dụng.
- Không thể xóa role còn permission hoặc còn được gán cho user.
- Replace chạy trong transaction: mọi ID được kiểm tra trước khi xóa dữ liệu cũ.
- Không dùng cascade delete để âm thầm làm mất cấu hình phân quyền.

## 13. Gợi ý tích hợp frontend

1. Sau đăng nhập, gọi `GET /api/me/permissions`.
2. Lưu cây permission trong state của phiên đăng nhập.
3. Hiển thị module/menu nếu cây response chứa module/group tương ứng.
4. Hiển thị button nếu có permission code cần thiết.
5. Scope chỉ hỗ trợ UI hiểu phạm vi; backend vẫn phải dùng scope khi query dữ liệu.
6. Sau khi quản trị viên đổi role hoặc override, frontend nên gọi lại `/api/me/permissions` ở request/phiên tiếp theo.

Ví dụ kiểm tra phía frontend:

```javascript
const hasPermission = (modules, permissionCode) =>
  modules.some(module =>
    module.groups.some(group =>
      group.permissions.some(permission => permission.code === permissionCode)
    )
  );
```

Không dùng kết quả này để thay thế kiểm tra authorization ở backend.
