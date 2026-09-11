# Employee Store Data Scope Design

## Objective

Add permission-aware employee data scoping to the existing multi-store administration system. Existing RBAC continues to answer whether an actor may perform an employee action; the new scope enforcement determines which employee records that permitted action may access.

The backend is the security boundary. Frontend scope-aware rendering improves clarity but never grants or restricts access by itself.

## Existing Architecture and Constraints

- `User` is the current employee/account record. No duplicate `Employee` entity will be introduced.
- `StoreStaff` is the current store assignment relation. An active assignment represents membership in a Store.
- An employee with no active `StoreStaff` assignment is a global employee.
- Employees holding `ADMIN` or `SUPER_ADMIN` must remain global and cannot receive a Store assignment.
- Employees holding another role are global when they have no active Store assignment and Store-scoped when they have one.
- `Department` is currently global. `Position` belongs to a global Department. Neither model will be redesigned or given a Store field in this change.
- Spring Security authorities and the existing `role_permissions`/`user_permissions` model remain authoritative for action-level authorization.
- Existing employee endpoint paths and request shapes remain compatible unless an explicit scope-context response is added.

## RBAC Scope Model

Add `STORE` to the existing `PermissionScope` enum and database constraints. The scope order is:

1. `SELF`
2. `TEAM`
3. `DEPARTMENT`
4. `STORE`
5. `ALL`

`PermissionScope.covers` retains its ordered behavior. For employee administration operations:

- effective permission scope `ALL` produces global employee data scope;
- effective permission scope `STORE` produces Store data scope;
- narrower effective scopes are also treated as Store data scope for this version, because the existing employee module has no safe query semantics for team/department ownership beyond the Store boundary;
- the normal action permission is still required by `@PreAuthorize`.

Default grants are migrated so `MANAGER` receives `STORE` for `USER_VIEW`, `USER_CREATE`, `USER_UPDATE`, and any other Employee permissions it holds. `ADMIN` and `SUPER_ADMIN` retain `ALL`. Custom roles and direct user grants can select `STORE` or `ALL` using the existing authorization management UI.

## Scope Resolution

Introduce a dedicated `EmployeeDataScopeService`. It resolves scope using the authenticated actor ID and the permission code for the current operation.

The result is an immutable value containing:

- whether access is global;
- the single Store ID and display data when Store-scoped;
- the permission code used to resolve scope.

Resolution rules:

1. Find the actor's effective permission for the requested action.
2. If it is absent, deny access. `@PreAuthorize` remains the first controller-level check, while the service check prevents internal callers from bypassing scope resolution.
3. If the effective scope is `ALL`, return global scope.
4. Otherwise, load distinct active Store assignments for the actor.
5. If there is exactly one active Store, return Store scope.
6. If none exist, reject the operation with a clear configuration error.
7. If more than one distinct active Store exists, reject the operation as ambiguous instead of silently broadening access.

No controller or business service may determine global access from a role name.

## Query Enforcement

Employee list/search queries accept a server-resolved scope Store ID separately from the optional client Store filter.

- Global scope: the scope Store ID is absent; the optional client filter may select any active Store.
- Store scope: the scope Store ID is mandatory; a missing client Store filter resolves to that Store; a different client Store filter returns HTTP 403.
- Global employees have no active `StoreStaff` row and are therefore excluded from every Store-scoped query.
- Search keyword, role, status, sorting, pagination, and counts are combined with the scope condition in the database query.
- `totalElements` and `totalPages` are calculated from the already scoped query.

Summary statistics use database aggregate queries with the same scope predicate. The service must not load an unbounded employee page and aggregate in memory.

## Read and Mutation Enforcement

Every target-based operation resolves its own action permission scope and checks the target before returning or mutating data:

- detail;
- update;
- soft delete;
- restore;
- lock/unlock;
- list subordinates;
- assign subordinate;
- remove subordinate.

For Store scope, the target must have an active assignment to the actor's Store. Employees assigned to another Store and global employees are forbidden. Both manager and subordinate are checked for subordinate operations.

The project convention is HTTP 403 for cross-scope access and HTTP 404 only when the target record genuinely does not exist.

## Create Behavior

Creation resolves scope using `USER_CREATE`.

Global actor:

- may provide an active Store ID to create a Store employee;
- may omit Store ID to create a global employee;
- may create `ADMIN` or `SUPER_ADMIN` only when existing role-grant restrictions permit it;
- `ADMIN` and `SUPER_ADMIN` targets always ignore/reject Store assignment and remain global.

Store actor:

- must have exactly one active Store assignment;
- a missing Store ID is replaced with the actor's Store ID;
- the same Store ID is accepted;
- a different Store ID returns HTTP 403;
- cannot create `ADMIN` or `SUPER_ADMIN` under the existing privileged-role restriction;
- cannot create a global employee.

Department and Position validation remains global and continues to require that the selected Position belongs to the selected Department.

## Update Behavior

Update resolves scope using `USER_UPDATE` and checks the existing target before accepting changes.

Global actor:

- may move a non-admin employee between Stores;
- may remove a non-admin employee's Store assignment to make the employee global;
- may assign a Store to an existing global non-admin employee;
- must keep `ADMIN` and `SUPER_ADMIN` global.

Store actor:

- may update only an employee assigned to the actor's Store;
- missing request Store ID is normalized to the existing/actor Store;
- another Store ID returns HTTP 403;
- removal of the Store assignment returns HTTP 403;
- cannot update a global employee or privileged administrator.

Existing validation for roles, account uniqueness, employment type, management cycles, Department, and Position remains active.

## API Contract

Existing routes remain:

- `GET /api/employees`
- `GET /api/employees/summary`
- `GET /api/employees/{id}`
- `POST /api/employees`
- `PUT /api/employees/{id}`
- `DELETE /api/employees/{id}`
- `PATCH /api/employees/{id}/lock`
- `PATCH /api/employees/{id}/restore`
- subordinate routes beneath `/api/employees/{id}/subordinates`
- `GET /api/employees/available-stores`

Add `GET /api/employees/scope`, protected by `USER_VIEW`. Its response contains:

- `scope`: `ALL` or `STORE`;
- `storeId`, `storeCode`, and `storeName` for Store scope;
- null Store fields for global scope.

The client-provided Store filter is never used to determine authorization.

## Frontend Behavior

The Employee Management page loads `/api/employees/scope` alongside its relation data.

Global view:

- header explains that all Stores are visible;
- Store filter remains available;
- Store column remains visible;
- create/edit Store selector includes a `Toàn chuỗi / Không thuộc cửa hàng` option for non-admin roles;
- statistics follow the selected Store filter or show all Stores when no filter is selected.

Store view:

- header and a compact badge display the current Store name and `Store scope` context;
- Store filter is hidden;
- list, search, role/status filters, pagination, counters, and summary rely on backend-scoped results;
- Store column may remain visible to preserve the current table layout and reinforce context;
- create/edit dialog shows the current Store as a disabled/read-only field;
- submitted data uses the current Store ID, while backend validation remains authoritative.

The frontend derives action visibility from the existing permission context. It does not use `ADMIN` or `SUPER_ADMIN` role names to determine actor data scope.

The authorization-management scope selector adds `STORE` with the label `Cửa hàng`.

## Error Handling

- Missing Store assignment for a Store-scoped actor: HTTP 403 with a configuration-focused message.
- Multiple active Store assignments for a Store-scoped actor: HTTP 403 with an ambiguity message.
- Client requests another Store under Store scope: HTTP 403.
- Cross-Store or global target access under Store scope: HTTP 403.
- Invalid/nonexistent Store under global scope: existing not-found or validation convention.
- Frontend displays backend error messages in the existing notice/dialog alert surfaces.

## Migration and Seed Maintenance

A new Flyway migration will:

- update the role/user permission scope check constraints to include `STORE`;
- migrate Employee permission grants for the built-in `MANAGER` role from `ALL` to `STORE`;
- preserve `ALL` for `ADMIN` and `SUPER_ADMIN`.

The canonical `db.sql` and idempotent authorization seed are updated to match, so rebuilding or reseeding a database cannot restore unsafe `ALL` grants for Store managers.

## Testing Strategy

Service-level tests use real scope-resolution behavior with mocked repositories only at persistence boundaries. Controller tests continue to verify `@PreAuthorize` and principal propagation.

Required coverage:

- `ALL` resolution for global HR permissions;
- `STORE` resolution with exactly one active Store;
- denial when Store scope has no Store assignment;
- denial when Store scope has multiple active Stores;
- global list can see Store A, Store B, and global employees;
- Store A list excludes Store B and global employees;
- Store B list excludes Store A and global employees;
- Store-filter mismatch returns forbidden before query execution;
- Store A detail of Store B/global employee returns forbidden;
- global creation for Store A, Store B, and no Store succeeds;
- Store A creation with missing/Same Store uses Store A;
- Store A creation targeting Store B returns forbidden;
- Store A update/delete/lock/restore of Store B returns forbidden;
- Store A cannot remove/change Store assignment;
- subordinate operations enforce scope for both records;
- summary repository receives the resolved Store scope and reports only scoped counts;
- Employee frontend builds with global and Store UI branches;
- authorization UI accepts the new `STORE` enum value.

Verification commands are the project's complete backend test/build commands and frontend build/lint commands. Pre-existing failures outside files changed by this feature will be reported separately with exact evidence.

## Out of Scope

- Adding Store ownership to Department or Position.
- Redesigning task, payroll, turnover, or unrelated reporting modules that do not currently expose Employee Administration APIs.
- Introducing a second authorization framework.
- Changing authentication token format.
- Supporting one Store-scoped HR account managing multiple Stores; that requires a separate explicit multi-Store scope design.
