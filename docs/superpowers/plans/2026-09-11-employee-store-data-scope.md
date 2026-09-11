# Employee Store Data Scope Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce permission-aware global/Store data scope across every Employee Management API and present that server-resolved scope consistently in the React admin UI.

**Architecture:** Existing RBAC continues to authorize actions through `USER_*` permissions. A new `EmployeeDataScopeService` verifies that permission, treats Admin/Super Admin as global, and for every other role resolves Store versus global from active `StoreStaff` assignment; `EmployeeAdministrationService` applies the immutable result to database queries and every target mutation. The frontend consumes a read-only `/api/employees/scope` context for presentation; it never supplies authorization scope.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Security method authorization, Spring Data JPA/PostgreSQL, Flyway-style SQL migrations, JUnit 5/Mockito, React 19, Vite 8, Tailwind CSS 4, ESLint 10.

**Spec:** `docs/superpowers/specs/2026-09-11-employee-store-data-scope-design.md`

## Global Constraints

- `ADMIN` and `SUPER_ADMIN` remain global and cannot receive an active Store assignment.
- Other employees with one active `StoreStaff` assignment are Store-scoped; those without one are global. Do not add a duplicate `store_id` to `users`.
- Backend is the security boundary; the client-provided `storeId` is only a filter.
- Existing `@PreAuthorize` RBAC checks remain in force; data scope is an additional restriction.
- Keep Department global and Position scoped only to Department in this change.
- Cross-scope access returns HTTP 403; genuine missing records retain HTTP 404.
- Do not add a UI framework or dependency, change API routes other than adding `/api/employees/scope`, or alter authentication/token behavior.
- Preserve unrelated working-tree and staged changes; commits use `git commit --only` with explicit feature paths.

---

## File Structure

- Create `dto/employee/EmployeeScopeResponse.java`: public response for the authenticated actor's Employee scope.
- Create `service/EmployeeDataScope.java`: immutable internal scope value with `ALL`/`STORE`, Store identity, and permission code.
- Create `service/EmployeeDataScopeService.java`: the only resolver and validator for Employee data scope.
- Create `service/EmployeeDataScopeServiceTest.java`: focused unit coverage for scope resolution and filter/target checks.
- Create `db/migration/V13__employee_store_permission_scope.sql`: schema constraint and built-in Manager grant migration.
- Modify `entity/PermissionScope.java`: add `STORE` between `DEPARTMENT` and `ALL`.
- Modify `service/AuthorizationService.java`: expose the effective scope for one permission without duplicating merge logic.
- Modify `repository/StoreStaffRepository.java`: deterministic distinct active Store lookup.
- Modify `repository/UserRepository.java`: database-scoped list and aggregate summary queries.
- Modify `service/EmployeeAdministrationService.java`: resolve per-operation scope and enforce it for all reads/mutations.
- Modify `controller/EmployeeAdministrationController.java`: remove role-derived booleans and expose `/scope`.
- Modify the existing Employee service/controller tests for the new interfaces and mandatory security cases.
- Modify `db/db.sql` and `db/seed_authorization.sql`: keep fresh installs and idempotent seeds aligned with V13.
- Modify `frontend/react-app/src/hooks/adminManagementApi.js`: add the scope request.
- Modify `frontend/react-app/src/pages/admin/EmployeeManagement.jsx`: load and render backend scope context.
- Modify `frontend/react-app/src/components/admin/Empolyee/EmployeeDialog.jsx`: global/store-aware Store control and payload normalization.
- Modify `frontend/react-app/src/components/admin/Role/GroupPermissionInfor.jsx`: expose `STORE` in authorization scope selection.

### Task 1: Add the STORE Authorization Scope Safely

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/PermissionScope.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V13__employee_store_permission_scope.sql`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/seed_authorization.sql`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/AuthorizationServiceTest.java`

**Interfaces:**
- Produces: `PermissionScope.STORE`; ordering `SELF < TEAM < DEPARTMENT < STORE < ALL`.
- Produces: persisted `STORE` values accepted by both `role_permissions.scope` and `user_permissions.scope`.

- [ ] **Step 1: Add a failing enum-order test**

```java
@Test
void storeScopeIsBroaderThanDepartmentAndNarrowerThanAll() {
    assertTrue(PermissionScope.STORE.covers(PermissionScope.DEPARTMENT));
    assertTrue(PermissionScope.ALL.covers(PermissionScope.STORE));
    assertFalse(PermissionScope.STORE.covers(PermissionScope.ALL));
}
```

- [ ] **Step 2: Run the focused test and verify it fails to compile because `STORE` does not exist**

Run from `backend/fashion-system`: `mvn -q -Dtest=AuthorizationServiceTest test`

- [ ] **Step 3: Add `STORE` in the enum's ordered position**

```java
public enum PermissionScope {
    SELF,
    TEAM,
    DEPARTMENT,
    STORE,
    ALL;
}
```

- [ ] **Step 4: Add V13 migration and synchronize install/seed SQL**

V13 must drop and recreate both scope checks with `('SELF','TEAM','DEPARTMENT','STORE','ALL')`, then update only built-in `MANAGER` Employee grants:

```sql
UPDATE role_permissions rp
SET scope = 'STORE', updated_at = CURRENT_TIMESTAMP
FROM roles r, permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND r.code = 'MANAGER'
  AND p.module = 'EMPLOYEE'
  AND p.code IN ('USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE')
  AND rp.effect = 'ALLOW';
```

Use the exact existing constraint names `chk_role_permissions_scope` and `chk_user_permissions_scope`. In `seed_authorization.sql`, split the final role-permission insert so Manager Employee grants use `STORE`, while Admin/Super Admin and non-Employee grants keep their current values.

- [ ] **Step 5: Run the focused authorization tests**

Run: `mvn -q -Dtest=AuthorizationServiceTest test`
Expected: PASS.

- [ ] **Step 6: Commit only Task 1 files**

```powershell
git commit --only backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/PermissionScope.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V13__employee_store_permission_scope.sql backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/db.sql backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/seed_authorization.sql backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/AuthorizationServiceTest.java -m "feat: add store permission scope"
```

### Task 2: Build the Shared Employee Scope Resolver

**Files:**
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeDataScope.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeDataScopeService.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/employee/EmployeeScopeResponse.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthorizationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/StoreStaffRepository.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeDataScopeServiceTest.java`

**Interfaces:**
- Consumes: `AuthorizationService.getPermissionScope(UUID userId, String permissionCode): Optional<PermissionScope>`.
- Produces: `EmployeeDataScope(EmployeeDataScope.Kind kind, UUID storeId, String storeCode, String storeName, String permissionCode)` with nested enum `Kind { ALL, STORE }`, factories `all(permissionCode)` and `store(store, permissionCode)`, and `boolean isGlobal()`.
- Produces: `EmployeeDataScopeService.resolve(UUID actorId, String permissionCode)`.
- Produces: `UUID validateFilter(EmployeeDataScope scope, UUID requestedStoreId)` returning the effective query Store filter.
- Produces: `void requireTarget(EmployeeDataScope scope, UUID targetUserId)`.
- Produces: `EmployeeScopeResponse(String scope, UUID storeId, String storeCode, String storeName)`.

- [ ] **Step 1: Write resolver tests for Admin, one Store, no Store, and multiple Stores**

```java
@Test
void adminResolvesGlobalWithoutLoadingAssignments() {
    when(authorizationService.getPermissionScope(actorId, "USER_VIEW"))
            .thenReturn(Optional.of(PermissionScope.ALL));
    when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("ADMIN"));
    EmployeeDataScope result = service.resolve(actorId, "USER_VIEW");
    assertTrue(result.isGlobal());
    assertNull(result.storeId());
    verifyNoInteractions(storeStaffRepository, storeRepository);
}

@Test
void storePermissionResolvesExactlyOneActiveStore() {
    when(authorizationService.getPermissionScope(actorId, "USER_VIEW"))
            .thenReturn(Optional.of(PermissionScope.STORE));
    when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
    when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId))
            .thenReturn(List.of(StoreStaff.builder().userId(actorId).storeId(storeId).active(true).build()));
    when(storeRepository.findById(storeId)).thenReturn(Optional.of(
            Store.builder().id(storeId).code("A").name("Store A").active(true).build()));
    EmployeeDataScope result = service.resolve(actorId, "USER_VIEW");
    assertFalse(result.isGlobal());
    assertEquals(storeId, result.storeId());
    assertEquals("Store A", result.storeName());
}

@Test
void nonAdminWithoutAssignmentResolvesGlobal() {
    when(authorizationService.getPermissionScope(actorId, "USER_VIEW"))
            .thenReturn(Optional.of(PermissionScope.STORE));
    when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
    when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId)).thenReturn(List.of());
    EmployeeDataScope result = service.resolve(actorId, "USER_VIEW");
    assertTrue(result.isGlobal());
}

@Test
void storePermissionWithMultipleAssignmentsIsForbidden() {
    UUID otherStoreId = UUID.randomUUID();
    when(authorizationService.getPermissionScope(actorId, "USER_VIEW"))
            .thenReturn(Optional.of(PermissionScope.STORE));
    when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
    when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId)).thenReturn(List.of(
            StoreStaff.builder().userId(actorId).storeId(storeId).active(true).build(),
            StoreStaff.builder().userId(actorId).storeId(otherStoreId).active(true).build()));
    assertThrows(BusinessException.class, () -> service.resolve(actorId, "USER_VIEW"));
}

@Test
void missingPermissionIsForbiddenEvenForInternalCaller() {
    when(authorizationService.getPermissionScope(actorId, "USER_VIEW")).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.resolve(actorId, "USER_VIEW"));
}
```

Stub `AuthorizationService.getPermissionScope`, `RoleRepository.findCodesByUserId`, `StoreStaffRepository.findAllByUserIdAndActiveTrue`, and `StoreRepository.findById`. Assert the ambiguous multi-Store message is configuration-focused.

- [ ] **Step 2: Run the new test class and verify failure**

Run: `mvn -q -Dtest=EmployeeDataScopeServiceTest test`
Expected: FAIL because the resolver types do not exist.

- [ ] **Step 3: Expose effective permission scope through AuthorizationService**

```java
@Transactional(readOnly = true)
public Optional<PermissionScope> getPermissionScope(UUID userId, String permissionCode) {
    return findPermission(userId, permissionCode).map(EffectivePermissionDto::scope);
}
```

- [ ] **Step 4: Implement immutable scope types and resolver**

```java
public EmployeeDataScope resolve(UUID actorId, String permissionCode) {
    PermissionScope permissionScope = authorizationService.getPermissionScope(actorId, permissionCode)
        .orElseThrow(() -> BusinessException.forbidden("Bạn không có quyền quản lý nhân viên"));
    List<String> roleCodes = roleRepository.findCodesByUserId(actorId);
    if (roleCodes.stream().anyMatch(Set.of("ADMIN", "SUPER_ADMIN")::contains)) {
        return EmployeeDataScope.all(permissionCode);
    }
    List<UUID> storeIds = storeStaffRepository.findAllByUserIdAndActiveTrue(actorId).stream()
        .map(StoreStaff::getStoreId).distinct().toList();
    if (storeIds.isEmpty()) return EmployeeDataScope.all(permissionCode);
    if (storeIds.size() != 1) throw BusinessException.forbidden("Tài khoản được gán nhiều cửa hàng nên phạm vi quản lý không rõ ràng");
    Store store = storeRepository.findById(storeIds.getFirst())
        .filter(value -> Boolean.TRUE.equals(value.getActive()))
        .orElseThrow(() -> BusinessException.forbidden("Cửa hàng quản lý không còn hoạt động"));
    return EmployeeDataScope.store(store, permissionCode);
}
```

`validateFilter` returns the request Store for ALL; for STORE it returns the scope Store and throws 403 if a different ID was supplied. `requireTarget` uses `existsByUserIdAndStoreIdAndActiveTrue`; ALL returns immediately.

- [ ] **Step 5: Run resolver and authorization tests**

Run: `mvn -q -Dtest=EmployeeDataScopeServiceTest,AuthorizationServiceTest test`
Expected: PASS.

- [ ] **Step 6: Commit only Task 2 files**

```powershell
git commit --only backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeDataScope.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeDataScopeService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/employee/EmployeeScopeResponse.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/AuthorizationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/StoreStaffRepository.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeDataScopeServiceTest.java -m "feat: resolve employee data scope"
```

### Task 3: Scope Employee List, Pagination, Stores, and Statistics in Database Queries

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRepository.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationServiceTest.java`

**Interfaces:**
- Consumes: `EmployeeDataScopeService.resolve(actorId, "USER_VIEW")` and `validateFilter`.
- Produces: `UserRepository.searchEmployees(UUID scopeStoreId, UUID filterStoreId, String keyword, String roleCode, String status, Pageable pageable)` where null `scopeStoreId` means global.
- Produces: `UserRepository.summarizeEmployees(UUID scopeStoreId, UUID filterStoreId, LocalDateTime monthStart): EmployeeSummaryResponse`.
- Produces: service methods `getList(UUID actorId, UUID requestedStoreId, String keyword, String roleCode, String status, Pageable pageable)`, `getSummary(UUID actorId, UUID requestedStoreId)`, `getAvailableStores(UUID actorId)`, and `getScope(UUID actorId)`.

- [ ] **Step 1: Replace boolean-based list tests with explicit scope tests**

Add tests that mock `EmployeeDataScopeService` and capture repository arguments:

Add methods named `storeListAlwaysPassesResolvedStoreToRepository`, `globalListMayFilterStoreB`, `storeListRejectsDifferentFilterBeforeRepositoryCall`, `summaryUsesAggregateQueryWithSameScope`, and `storeAvailableStoresReturnsOnlyResolvedStore`. In order, verify repository calls `searchEmployees(storeA, storeA, "", "", "", pageable)`, `searchEmployees(null, storeB, "", "", "", pageable)`, no search after a 403 mismatch, `summarizeEmployees(storeA, storeA, capturedMonthStart)` with no `searchEmployees` call, and one returned Store DTO whose ID is Store A.

- [ ] **Step 2: Run the focused service tests and verify signature/query failures**

Run: `mvn -q -Dtest=EmployeeAdministrationServiceTest test`

- [ ] **Step 3: Rewrite repository predicates around server scope**

The mandatory predicate is:

```java
(:scopeStoreId is null or exists (
  select scoped.id from StoreStaff scoped
  where scoped.userId = u.id
    and scoped.storeId = :scopeStoreId
    and scoped.active = true
))
```

Keep the optional `filterStoreId` predicate separate and combine it with keyword, role, status, sort, and pageable in the same query. Remove `actorId` and `allStores` from repository query parameters.

- [ ] **Step 4: Add database aggregate summary query**

```java
@Query("""
select new com.fashionsystem.fashion_system.dto.employee.EmployeeSummaryResponse(
  count(distinct u.id),
  count(distinct case when u.deletedAt is null and u.active = true then u.id end),
  count(distinct case when u.deletedAt is null and u.locked = true then u.id end),
  count(distinct case when u.createdAt >= :monthStart then u.id end))
from User u
where (:scopeStoreId is null or exists (
  select scoped.id from StoreStaff scoped
  where scoped.userId = u.id and scoped.storeId = :scopeStoreId and scoped.active = true))
  and (:filterStoreId is null or exists (
  select filtered.id from StoreStaff filtered
  where filtered.userId = u.id and filtered.storeId = :filterStoreId and filtered.active = true))
""")
EmployeeSummaryResponse summarizeEmployees(UUID scopeStoreId, UUID filterStoreId, LocalDateTime monthStart);
```

Use the complete StoreStaff `exists` clauses from `searchEmployees`; do not join assignments directly because that can duplicate employees and corrupt counts.

- [ ] **Step 5: Refactor read services to resolve `USER_VIEW` internally**

Remove every `boolean privileged` argument. `getSummary` calls only `summarizeEmployees`, not `PageRequest.of(0, Integer.MAX_VALUE)`. `getAvailableStores` returns all active Stores only for ALL; for STORE it maps the resolved Store. `getScope` maps the same resolved context to `EmployeeScopeResponse`.

- [ ] **Step 6: Run Employee service tests**

Run: `mvn -q -Dtest=EmployeeAdministrationServiceTest,EmployeeDataScopeServiceTest test`
Expected: PASS with repository scope arguments verified.

- [ ] **Step 7: Commit Task 3 files**

```powershell
git commit --only backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRepository.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationServiceTest.java -m "feat: scope employee queries and statistics"
```

### Task 4: Enforce Scope on Employee Creation and Every Target Mutation

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationServiceTest.java`

**Interfaces:**
- Consumes: action-specific `resolve(actorId, USER_CREATE|USER_UPDATE|USER_DELETE)`.
- Consumes: `AuthorizationService.hasPermission(actorId, "USER_CREATE_ADMIN")` and existing `RoleRepository.findCodesByUserId(actorId)` for privileged-role grant restrictions only, not actor data scope.
- Produces: `normalizeCreateStore(EmployeeDataScope scope, UUID requestStoreId, Set<String> roleCodes)` and `normalizeUpdateStore(EmployeeDataScope scope, UUID existingStoreId, UUID requestStoreId, Set<String> roleCodes)` private helpers.
- Produces: all public Employee methods accept actor ID plus business arguments only, never caller-supplied scope booleans.

- [ ] **Step 1: Add mandatory create tests**

Add methods named `globalActorCreatesEmployeesInStoreAStoreBAndNoStore`, `storeActorCreateWithoutStoreUsesActorsStore`, `storeActorCreateWithSameStoreSucceeds`, `storeActorCreateForOtherStoreIsForbiddenBeforeSave`, and `adminOrSuperAdminTargetNeverReceivesStoreAssignment`. Capture saved `StoreStaff` rows to assert Store A, Store B, and no assignment; assert both missing and matching Store input normalize to Store A; assert Store B raises `BusinessException` and `verify(userRepository, never()).save(any())`; assert administrator targets never call `storeStaffRepository.save`.

Continue requiring Department and Position for operational non-admin roles even when a global actor creates them without Store. Split `requiresOrganization(roleCodes)` from `isPrivilegedTarget(roleCodes)` so Store assignment no longer controls organization validation.

- [ ] **Step 2: Add target and update tests**

Add methods named `storeAActorCannotReadUpdateDeleteLockOrRestoreStoreBEmployee`, `storeAActorCannotAccessGlobalEmployeeByKnownUuid`, `storeAActorCannotRemoveOrChangeEmployeesStore`, `globalActorCanMoveNonAdminBetweenStoresOrMakeGlobal`, and `storeAActorCannotAssignStoreBManagerOrSubordinate`. Each cross-scope case must assert `BusinessException`; mutation cases must additionally verify no `userRepository.save`, `storeStaffRepository.save`, or `storeStaffRepository.delete` call. The global move test captures the replacement assignment for Store B, then verifies the null-Store update leaves no active assignment.

- [ ] **Step 3: Run focused tests and verify they fail**

Run: `mvn -q -Dtest=EmployeeAdministrationServiceTest test`

- [ ] **Step 4: Refactor create and update normalization**

Resolve `USER_CREATE` before validation. For STORE, null becomes actor Store, a mismatch throws 403, and operational target roles must get that Store. For ALL, null remains global. Privileged target roles always normalize to null Store and null Department/Position.

On update, load target, call `requireTarget` using `USER_UPDATE`, obtain the target's single active Store assignment, then reject Store removal/change for STORE actors. ALL may replace an operational employee's assignment. Replace controller booleans in `ensureRolesAllowed` with `AuthorizationService.hasPermission(actorId, "USER_CREATE_ADMIN")` for ADMIN grants and the existing `RoleRepository.findCodesByUserId(actorId).contains("SUPER_ADMIN")` rule for SUPER_ADMIN grants. Those checks preserve privileged-role assignment security and must not be reused to derive ordinary actor data scope outside `EmployeeDataScopeService`.

- [ ] **Step 5: Apply target checks to every operation**

Use `USER_VIEW` for detail/subordinate reads, `USER_UPDATE` for update/lock/restore/assign/remove subordinate, and `USER_DELETE` for delete. For subordinate assignment/removal call `requireTarget` for both manager and subordinate. Filter-free post-loading is not acceptable for any one-record route.

Fix the existing delete audit path by capturing `EmployeeResponse oldData = toResponse(employee)` before mutation and passing that defined value to `auditLogService.record`.

- [ ] **Step 6: Run all Employee service tests**

Run: `mvn -q -Dtest=EmployeeAdministrationServiceTest,EmployeeDataScopeServiceTest test`
Expected: PASS, including no persistence call on rejected cross-scope requests.

- [ ] **Step 7: Commit Task 4 files**

```powershell
git commit --only backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationServiceTest.java -m "feat: enforce scope on employee mutations"
```

### Task 5: Remove Controller Role Heuristics and Publish Scope Context

**Files:**
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationController.java`
- Modify: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationControllerSecurityTest.java`

**Interfaces:**
- Consumes: scope-aware service signatures from Tasks 3 and 4.
- Produces: `GET /api/employees/scope` guarded by `hasAuthority('USER_VIEW')`.
- Produces: controller forwarding only `AuthenticatedUser.userId()` and request business parameters.

- [ ] **Step 1: Rewrite controller tests around RBAC and principal propagation**

Add methods named `missingUserViewCannotReadScopeOrList`, `sameUserViewAuthorityUsesSameServiceSignatureRegardlessOfRoleName`, `scopeEndpointUsesAuthenticatedActorId`, and `createUpdateDeleteKeepTheirExistingAuthorities`. Assert missing authorities throw `AccessDeniedException` and cause no service interaction; authenticate once with `ROLE_MANAGER,USER_VIEW` and once with `ROLE_ADMIN,USER_VIEW` and verify the identical `getList(actorId, null, null, null, null, pageable)` call; verify `/scope` calls `getScope(actorId)`; verify create/update/delete each require their existing `USER_CREATE`, `USER_UPDATE`, and `USER_DELETE` authority.

- [ ] **Step 2: Run controller tests and verify failure against old boolean signatures**

Run: `mvn -q -Dtest=EmployeeAdministrationControllerSecurityTest test`

- [ ] **Step 3: Remove `isPrivileged`, `isSuperAdmin`, and role checks from controller**

Add:

```java
@GetMapping("/scope")
@PreAuthorize("hasAuthority('USER_VIEW')")
public EmployeeScopeResponse getScope(Authentication authentication) {
    return service.getScope(userId(authentication));
}
```

Update every other call to pass no role-derived boolean. Preserve current paths, HTTP methods, response statuses, and `@PreAuthorize` authorities.

- [ ] **Step 4: Run controller and service tests**

Run: `mvn -q -Dtest=EmployeeAdministrationControllerSecurityTest,EmployeeAdministrationServiceTest,EmployeeDataScopeServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit Task 5 files**

```powershell
git commit --only backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationController.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationControllerSecurityTest.java -m "feat: expose employee scope context"
```

### Task 6: Make Employee Management UI Use Backend Scope

**Files:**
- Modify: `frontend/react-app/src/hooks/adminManagementApi.js`
- Modify: `frontend/react-app/src/pages/admin/EmployeeManagement.jsx`
- Modify: `frontend/react-app/src/components/admin/Empolyee/EmployeeDialog.jsx`

**Interfaces:**
- Consumes: `{ scope: "ALL"|"STORE", storeId, storeCode, storeName }` from `/api/employees/scope`.
- Produces: `employeeApi.scope(options)`.
- Produces: `EmployeeDialog` prop `scopeContext`; it controls only display/default payload while backend remains authoritative.

- [ ] **Step 1: Add scope API and load it with relation data**

```javascript
scope: (options = {}) => requestAdmin("/api/employees/scope", options),
```

In the initial `Promise.all`, request scope before stores/departments/positions and store it in state. Do not infer scope from `user.roles`; keep role inspection only where needed for existing privileged-role creation visibility.

- [ ] **Step 2: Normalize filters after scope loads**

For `STORE`, set `filters.storeId` to `""` and never send a Store selector value from filters; the backend applies actor Store automatically. For `ALL`, retain the Store filter and “Tất cả cửa hàng”. Reset preserves the same rule. Summary receives only the optional global filter.

- [ ] **Step 3: Render explicit scope context**

Use `scopeContext.scope === "ALL"` for the page description. For STORE, show a compact warm-orange badge containing `Store scope · {storeName}` near the header/toolbar and hide the Store filter. Keep the Store table column to reinforce context and avoid changing table structure.

- [ ] **Step 4: Make EmployeeDialog's Store field scope-aware**

For operational roles:

- ALL: Store select includes `<option value="">Toàn chuỗi / Không thuộc cửa hàng</option>` and all available Stores.
- STORE: render the current Store name in a disabled/readonly control and force submitted `storeId` to `scopeContext.storeId`.
- ADMIN/SUPER_ADMIN target: submit `storeId: null`, `departmentId: null`, and `positionId: null` as today.

Update validation so Store is required only when `scopeContext.scope === "STORE"` and the target is operational. Initial form uses the current Store for STORE scope and does not silently choose the first Store for ALL scope.

- [ ] **Step 5: Run lint on only changed frontend files**

Run from `frontend/react-app`:

```powershell
npx eslint src/hooks/adminManagementApi.js src/pages/admin/EmployeeManagement.jsx src/components/admin/Empolyee/EmployeeDialog.jsx
```

Expected: PASS.

- [ ] **Step 6: Run frontend production build**

Run: `node node_modules/vite/bin/vite.js build`
Expected: Vite build succeeds.

- [ ] **Step 7: Commit Task 6 files**

```powershell
git commit --only frontend/react-app/src/hooks/adminManagementApi.js frontend/react-app/src/pages/admin/EmployeeManagement.jsx frontend/react-app/src/components/admin/Empolyee/EmployeeDialog.jsx -m "feat: present employee store scope"
```

### Task 7: Expose STORE in Authorization Administration

**Files:**
- Modify: `frontend/react-app/src/components/admin/Role/GroupPermissionInfor.jsx`

**Interfaces:**
- Consumes/produces persisted scope string `STORE` through the existing selected-permission mapping.

- [ ] **Step 1: Add STORE at the correct visual order**

```javascript
const SCOPES = [
  ["SELF", "Cá nhân"],
  ["TEAM", "Nhóm trực thuộc"],
  ["DEPARTMENT", "Phòng ban"],
  ["STORE", "Cửa hàng"],
  ["ALL", "Toàn hệ thống"],
];
```

- [ ] **Step 2: Run focused lint and build**

Run from `frontend/react-app`:

```powershell
npx eslint src/components/admin/Role/GroupPermissionInfor.jsx
node node_modules/vite/bin/vite.js build
```

Expected: both succeed.

- [ ] **Step 3: Commit Task 7 file**

```powershell
git commit --only frontend/react-app/src/components/admin/Role/GroupPermissionInfor.jsx -m "feat: allow store permission grants"
```

### Task 8: Full Security Regression and Build Verification

**Files:**
- Test: all backend tests under `backend/fashion-system/src/test`
- Test: all frontend source under `frontend/react-app/src`
- Review: every file changed in Tasks 1–7

**Interfaces:**
- Consumes: complete backend and frontend feature.
- Produces: evidence that RBAC, data scope, builds, and changed-file lint pass without touching unrelated changes.

- [ ] **Step 1: Run the complete backend test suite**

Run from `backend/fashion-system`: `mvn test -q`
Expected: BUILD SUCCESS and all tests pass. If a failure is caused by this feature, diagnose and fix it before proceeding.

- [ ] **Step 2: Run a clean backend compile**

Run: `mvn -q -DskipTests clean compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Run frontend build and changed-file lint**

Run from `frontend/react-app`:

```powershell
node node_modules/vite/bin/vite.js build
npx eslint src/hooks/adminManagementApi.js src/pages/admin/EmployeeManagement.jsx src/components/admin/Empolyee/EmployeeDialog.jsx src/components/admin/Role/GroupPermissionInfor.jsx
```

Expected: both pass. Also run `npm run lint`; if it still reports unrelated pre-existing findings, record the exact count and confirm none originate in feature files.

- [ ] **Step 4: Inspect scope bypass patterns and diff hygiene**

Run from repository root:

```powershell
rg -n "isPrivileged|allStores|PageRequest\.of\(0, Integer\.MAX_VALUE\)" backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationController.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRepository.java
git diff --check
git status --short
```

Expected: no role-derived Employee scope or unbounded summary query remains; `git diff --check` is clean. Review status without unstaging, deleting, or committing unrelated user changes.

- [ ] **Step 5: Manually verify the required authorization matrix if a local backend/database is available**

Use three test identities: global HR, Store A HR, Store B HR. Confirm global list/create across A/B/global; A/B isolation for list/detail/create/update/delete/lock/restore/subordinates; Store filter mismatch 403; scoped summary totals and pagination counts. If no runnable seeded database is available, report that manual HTTP verification was not run and rely on the automated service/controller cases rather than fabricating results.

- [ ] **Step 6: Commit any verification-driven corrections with explicit paths**

Use `git commit --only` followed by the explicit paths corrected during verification and message `fix: close employee scope regressions`; never use blanket `git add .` because the worktree contains unrelated staged changes.
