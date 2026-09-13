# Position Hierarchy and Salary Range Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add immutable globally unique position codes, hierarchy levels, VND salary ranges, same-department reporting rules, and confirmation-driven cleanup when an employee or position change invalidates reporting lines.

**Architecture:** Keep `positions` as the source of truth for department, hierarchy, and salary range. Introduce a focused `OrganizationHierarchyService` that validates manager/subordinate pairs and calculates invalid reporting lines for proposed mutations. Preview endpoints support the UI, while every write recalculates impact inside the transaction and requires an explicit `resetInvalidRelations` flag before clearing invalid `users.manager_id` values.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, Jakarta Validation, Flyway SQL, Redis/Spring Cache, JUnit 5/Mockito, React 19, Vite, Node test runner, `SystemNotification`.

**Spec:** `docs/superpowers/specs/2026-09-13-position-hierarchy-salary-design.md`

## Global Constraints

- Preserve all unrelated staged and unstaged user changes. If commits are created, use `git commit --only -- <explicit paths>` and verify the index before and after every commit.
- A higher numeric `hierarchyLevel` means a higher position. Equal levels cannot manage each other.
- A manager and subordinate must have non-deleted employee records, positions in the same department, and `manager.hierarchyLevel > subordinate.hierarchyLevel`. Retain the existing assignment-time active/unlocked-subordinate, full-time-manager, and cycle checks.
- Position salary values are integer VND stored as Java `Long`: `minSalary >= 0` and `maxSalary >= minSalary`.
- Position codes are globally unique, normalized with `trim().toUpperCase(Locale.ROOT)`, immutable after creation, protected by both application validation and a database unique constraint. Duplicate creation returns HTTP 409.
- Never use `window.alert`, `window.confirm`, or inline success/error banners for the touched position, employee-edit, and subordinate-management flows. Use `useSystemNotification()` for errors, success messages, and confirmations.
- Redis may cache position detail reads, but hierarchy validation and impact calculation must always read transactionally from the database.

---

## Task 1: Persist hierarchy and salary fields safely

**Files:**

- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V16__position_hierarchy_and_salary.sql`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/Position.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/PositionDto.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/mapper/PositionMapperTest.java`

- [ ] Write a failing mapper test proving `hierarchyLevel`, `minSalary`, and `maxSalary` are included in the response DTO.

```java
@Test
void toDtoIncludesHierarchyAndSalaryRange() {
    Position position = Position.builder()
            .id(UUID.randomUUID())
            .departmentId(UUID.randomUUID())
            .code("LEAD")
            .name("Trưởng nhóm")
            .hierarchyLevel(3)
            .minSalary(12_000_000L)
            .maxSalary(18_000_000L)
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

    PositionDto dto = new PositionMapper().toDto(position, null);

    assertThat(dto.getHierarchyLevel()).isEqualTo(3);
    assertThat(dto.getMinSalary()).isEqualTo(12_000_000L);
    assertThat(dto.getMaxSalary()).isEqualTo(18_000_000L);
}
```

- [ ] Run the focused test and confirm it fails because the fields do not exist.

```powershell
cd backend/fashion-system
mvn -q -Dtest=PositionMapperTest test
```

- [ ] Add the migration with compatibility defaults and database checks. `V9__positions_and_employee_organization.sql` already defines `code VARCHAR(50) UNIQUE NOT NULL`; preserve that global constraint and do not add a duplicate unique constraint in V16.

```sql
ALTER TABLE positions
    ADD COLUMN hierarchy_level INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN min_salary BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN max_salary BIGINT NOT NULL DEFAULT 0;

ALTER TABLE positions
    ADD CONSTRAINT ck_positions_hierarchy_level CHECK (hierarchy_level > 0),
    ADD CONSTRAINT ck_positions_min_salary CHECK (min_salary >= 0),
    ADD CONSTRAINT ck_positions_salary_range CHECK (max_salary >= min_salary);
```

- [ ] Add non-null entity fields with matching column names and builder defaults.

```java
@Column(name = "hierarchy_level", nullable = false)
@Builder.Default
private Integer hierarchyLevel = 1;

@Column(name = "min_salary", nullable = false)
@Builder.Default
private Long minSalary = 0L;

@Column(name = "max_salary", nullable = false)
@Builder.Default
private Long maxSalary = 0L;
```

- [ ] Add the three response fields to `PositionDto` without using it as a write request after Task 2.

- [ ] Update `PositionMapper.toDto` and run the mapper test until green.

- [ ] Commit only Task 1 paths.

```powershell
git commit --only -- backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/db/migration/V16__position_hierarchy_and_salary.sql backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/entity/Position.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/PositionDto.java backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/mapper/PositionMapper.java backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/mapper/PositionMapperTest.java -m "feat: persist position hierarchy and salary range"
```

---

## Task 2: Make position code immutable and globally unique at the API boundary

**Files:**

- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/position/CreatePositionRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/position/UpdatePositionRequest.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/PositionController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/mapper/PositionMapper.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PositionService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/PositionServiceTest.java`

- [ ] Add failing tests for normalized creation, duplicate-code HTTP conflict, immutable update, and salary validation.

```java
@Test
void createNormalizesAndRejectsDuplicateGlobalCode() {
    when(positionRepository.existsByCode("LEAD")).thenReturn(true);

    BusinessException error = assertThrows(BusinessException.class,
            () -> service.create(new CreatePositionRequest(
                    departmentId, " lead ", "Trưởng nhóm", null, true, 3, 12_000_000L, 18_000_000L)));

    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
}

@Test
void updateNeverChangesCode() {
    Position existing = position("LEAD", 3, 12_000_000L, 18_000_000L);
    when(positionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

    service.update(existing.getId(), new UpdatePositionRequest(
            departmentId, "Tên mới", null, true, 4, 15_000_000L, 20_000_000L, false));

    assertThat(existing.getCode()).isEqualTo("LEAD");
}
```

- [ ] Define request records with Jakarta validation. `UpdatePositionRequest` deliberately has no `code` component.

```java
public record CreatePositionRequest(
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        String description,
        Boolean active,
        @NotNull @Positive Integer hierarchyLevel,
        @NotNull @PositiveOrZero Long minSalary,
        @NotNull @PositiveOrZero Long maxSalary) {}

public record UpdatePositionRequest(
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 150) String name,
        String description,
        Boolean active,
        @NotNull @Positive Integer hierarchyLevel,
        @NotNull @PositiveOrZero Long minSalary,
        @NotNull @PositiveOrZero Long maxSalary,
        Boolean resetInvalidRelations) {}
```

- [ ] Change controller and service signatures to use the dedicated requests. Validate `maxSalary >= minSalary` in `PositionService` and map invalid ranges to HTTP 400.

- [ ] Split mapper writes into `toEntity(CreatePositionRequest)` and `updateEntity(UpdatePositionRequest, Position)`. Only `toEntity` sets normalized `code`.

- [ ] Retain `ensureCodeAvailable` only in create. Catch a concurrent `DataIntegrityViolationException` from `saveAndFlush` and translate it to `BusinessException.conflict("Mã vị trí đã tồn tại")` so the DB constraint remains race-safe.

- [ ] Run the focused service test.

```powershell
cd backend/fashion-system
mvn -q -Dtest=PositionServiceTest test
```

- [ ] Commit only Task 2 paths.

---

## Task 3: Cache position detail reads with Redis

**Files:**

- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/CacheNames.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/config/RedisCacheConfig.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PositionService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/config/RedisCacheConfigTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/PositionServiceTest.java`

- [ ] Add failing cache registration and annotation-reflection tests for `POSITION_DETAIL`.

```java
assertThat(config.cacheConfigurations()).containsKey(CacheNames.POSITION_DETAIL);
assertThat(PositionService.class.getMethod("getById", UUID.class)
        .getAnnotation(Cacheable.class).cacheNames()).containsExactly(CacheNames.POSITION_DETAIL);
```

- [ ] Register `CacheNames.POSITION_DETAIL = "reference.position.detail"` under catalog TTL.

- [ ] Annotate `getById` with `@Cacheable`, successful `update` with `@CachePut`, and `delete` with `@CacheEvict`, all keyed by position ID. Do not cache list or hierarchy-impact queries.

- [ ] Run cache and position tests.

```powershell
cd backend/fashion-system
mvn -q -Dtest=RedisCacheConfigTest,PositionServiceTest test
```

- [ ] Commit only Task 3 paths.

---

## Task 4: Build the hierarchy rule engine and structured confirmation response

**Files:**

- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/hierarchy/HierarchyImpactResponse.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/exception/HierarchyConfirmationRequiredException.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/OrganizationHierarchyService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRepository.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/OrganizationHierarchyServiceTest.java`

- [ ] Write failing tests for all pair rules: missing/deleted employee, missing position, different department, equal/lower manager level, non-full-time manager, valid higher manager, and cycle detection. Keep the existing assignment-flow test for rejecting an inactive or locked subordinate.

- [ ] Write failing impact tests covering an employee demotion, employee department move, position-level change affecting holders on either side of a relation, deduplication, and no-op changes.

- [ ] Add the public response record and a private/internal impact value carrying exact subordinate IDs to clear.

```java
public record HierarchyImpactResponse(
        long affectedRelationCount,
        long affectedEmployeeCount,
        String summary) {}

public record HierarchyImpact(
        Set<UUID> invalidSubordinateIds,
        Set<UUID> affectedEmployeeIds) {
    public boolean isEmpty() { return invalidSubordinateIds.isEmpty(); }
    public HierarchyImpactResponse toResponse() { /* return stable counts and Vietnamese summary */ }
}
```

- [ ] Implement service entry points with database entities as inputs so callers can evaluate proposed values before mutation.

```java
public void validateAssignment(User manager, User subordinate);
public HierarchyImpact analyzeEmployeeChange(User employee, Position proposedPosition);
public HierarchyImpact analyzePositionChange(Position position, UUID proposedDepartmentId, int proposedLevel);
public void confirmOrClear(HierarchyImpact impact, Boolean resetInvalidRelations);
```

- [ ] Add `UserRepository.findAllByPositionIdAndDeletedAtIsNull(UUID positionId)` and reuse `findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc`. Load managers by ID from the repository; do not call cached position reads.

- [ ] In `confirmOrClear`, throw a structured HTTP 409 when confirmation is absent. When confirmed, load each invalid subordinate, set only `managerId = null`, update `updatedAt`, and save once with deduplicated IDs.

```java
getBody().setProperty("code", "HIERARCHY_CONFIRMATION_REQUIRED");
getBody().setProperty("affectedRelationCount", impact.affectedRelationCount());
getBody().setProperty("affectedEmployeeCount", impact.affectedEmployeeCount());
```

- [ ] Ensure `affectedEmployeeCount` counts distinct managers and subordinates, while `affectedRelationCount` counts invalid subordinate-to-manager links.

- [ ] Run hierarchy unit tests.

```powershell
cd backend/fashion-system
mvn -q -Dtest=OrganizationHierarchyServiceTest test
```

- [ ] Commit only Task 4 paths.

---

## Task 5: Integrate confirmation-safe hierarchy changes into positions

**Files:**

- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/PositionController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/PositionService.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/PositionServiceTest.java`
- Create: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/PositionControllerSecurityTest.java`

- [ ] Add failing service tests showing that level/department changes are rejected with structured 409 without confirmation, confirmed changes clear only invalid relations, and name/salary-only changes do not invoke cleanup.

- [ ] Add the preview endpoint guarded by `POSITION_UPDATE`.

```java
@GetMapping("/{id}/hierarchy-impact")
@PreAuthorize("hasAuthority('POSITION_UPDATE')")
public HierarchyImpactResponse getHierarchyImpact(
        @PathVariable UUID id,
        @RequestParam UUID departmentId,
        @RequestParam @Positive Integer hierarchyLevel) {
    return service.getHierarchyImpact(id, departmentId, hierarchyLevel);
}
```

- [ ] Implement `PositionService.getHierarchyImpact` as read-only and uncached. In `update`, calculate impact before mutating the entity, call `confirmOrClear`, then update and save within the same transaction.

- [ ] Recalculate impact on every update request even after a preview. Never trust preview counts or client-supplied IDs.

- [ ] Add controller security tests for missing `POSITION_UPDATE`, valid preview access, and invalid numeric level validation.

- [ ] Run focused tests.

```powershell
cd backend/fashion-system
mvn -q -Dtest=PositionServiceTest,PositionControllerSecurityTest test
```

- [ ] Commit only Task 5 paths.

---

## Task 6: Integrate hierarchy rules into employee changes and subordinate assignment

**Files:**

- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/employee/UpdateEmployeeRequest.java`
- Create: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/dto/employee/EligibleSubordinateResponse.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationController.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationService.java`
- Modify: `backend/fashion-system/src/main/java/com/fashionsystem/fashion_system/repository/UserRepository.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/service/EmployeeAdministrationServiceTest.java`
- Test: `backend/fashion-system/src/test/java/com/fashionsystem/fashion_system/controller/EmployeeAdministrationControllerSecurityTest.java`

- [ ] Extend `UpdateEmployeeRequest` with a trailing nullable `Boolean resetInvalidRelations` and update existing constructor call sites.

- [ ] Add failing tests for editing an employee to a lower/equal position, moving departments, confirmed cleanup of incoming and outgoing invalid relations, and unaffected edits.

- [ ] Add failing subordinate-assignment tests that reject a different department and a manager whose level is equal to or lower than the subordinate; retain tests for full-time status, active/unlocked subordinate, existing manager, self-management, and cycles.

- [ ] Add a scoped eligible-candidate query to `UserRepository` and service tests proving it returns only active, unlocked, unassigned, non-deleted employees whose positions belong to the manager's department and have a strictly lower level. Apply the actor's existing store scope in the query.

```java
@Query("""
        select u from User u, Position p
        where u.positionId = p.id
          and u.id <> :managerId
          and u.deletedAt is null
          and u.active = true
          and u.locked = false
          and u.managerId is null
          and p.active = true
          and p.departmentId = :departmentId
          and p.hierarchyLevel < :managerLevel
          and (:scopeStoreId is null or exists (
              select staff.id from StoreStaff staff
              where staff.userId = u.id
                and staff.storeId = :scopeStoreId
                and staff.active = true
          ))
        order by u.fullName
        """)
List<User> findEligibleSubordinates(
        UUID managerId, UUID departmentId, Integer managerLevel, UUID scopeStoreId);
```

- [ ] Add the employee preview endpoint guarded by `USER_UPDATE` and the existing target data-scope check.

```java
@GetMapping("/{id}/hierarchy-impact")
@PreAuthorize("hasAuthority('USER_UPDATE')")
public HierarchyImpactResponse getHierarchyImpact(
        Authentication authentication,
        @PathVariable UUID id,
        @RequestParam UUID departmentId,
        @RequestParam UUID positionId) {
    return service.getHierarchyImpact(userId(authentication), id, departmentId, positionId);
}
```

- [ ] Add `GET /api/employees/{id}/eligible-subordinates`, guarded by `USER_UPDATE`. Resolve the manager's position and data scope server-side and return `List<EligibleSubordinateResponse>` from the scoped repository query; do not accept client-supplied department or level filters.

```java
public record EligibleSubordinateResponse(
        UUID id,
        String employeeCode,
        String fullName,
        String email,
        UUID positionId,
        String positionName,
        Integer hierarchyLevel) {}
```

- [ ] In employee update, validate the proposed organization first, calculate incoming/outgoing impact, require confirmation or clear invalid links, and only then change `positionId`, department membership, and job title in the same transaction.

- [ ] Replace the duplicated hierarchy logic in `assignSubordinate` with `organizationHierarchyService.validateAssignment(manager, subordinate)` while retaining data-scope authorization and audit logging.

- [ ] Run focused employee tests.

```powershell
cd backend/fashion-system
mvn -q -Dtest=EmployeeAdministrationServiceTest,EmployeeAdministrationControllerSecurityTest test
```

- [ ] Commit only Task 6 paths.

---

## Task 7: Add tested frontend payload, salary, and hierarchy helpers

**Files:**

- Create: `frontend/react-app/src/pages/admin/positionManagementLogic.js`
- Create: `frontend/react-app/src/pages/admin/positionManagementLogic.test.js`
- Modify: `frontend/react-app/src/hooks/adminManagementApi.js`

- [ ] Write failing Node tests for VND formatting, client validation, update payload omission of `code`, hierarchy-change detection, and structured-409 detection.

```javascript
test("update payload never sends position code", () => {
  const payload = toUpdatePositionPayload({
    code: "LEAD", departmentId: "dep-1", name: "Lead", active: true,
    hierarchyLevel: "3", minSalary: "12000000", maxSalary: "18000000",
  });
  assert.equal(Object.hasOwn(payload, "code"), false);
});

test("formats VND salary range", () => {
  assert.equal(formatSalaryRange(12000000, 18000000), "12.000.000 ₫ – 18.000.000 ₫");
});
```

- [ ] Implement pure helpers: `validatePositionForm`, `toCreatePositionPayload`, `toUpdatePositionPayload`, `hasHierarchyChange`, `formatSalaryRange`, and `isHierarchyConfirmationRequired`.

- [ ] Add API clients:

```javascript
positionApi.detail = (id, options = {}) => requestAdmin(`/api/positions/${id}`, options);
positionApi.hierarchyImpact = (id, params, options = {}) =>
  requestAdmin(`/api/positions/${id}/hierarchy-impact${queryString(params)}`, options);
employeeApi.hierarchyImpact = (id, params, options = {}) =>
  requestAdmin(`/api/employees/${id}/hierarchy-impact${queryString(params)}`, options);
employeeApi.eligibleSubordinates = (id, options = {}) =>
  requestAdmin(`/api/employees/${id}/eligible-subordinates`, options);
```

- [ ] Run helper tests.

```powershell
cd frontend/react-app
node --test src/pages/admin/positionManagementLogic.test.js
```

- [ ] Commit only Task 7 paths.

---

## Task 8: Upgrade position management and department position creation UI

**Files:**

- Modify: `frontend/react-app/src/pages/admin/PositionManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/DepartmentManagement.jsx`
- Modify: `frontend/react-app/src/pages/admin/positionManagementLogic.js`

- [ ] Add columns for hierarchy level and salary range to `/admin/positions` and display zero ranges consistently using the helper formatter.

- [ ] Add create/edit inputs for positive integer level and integer min/max VND. Show field-level validation before API calls.

- [ ] On edit, render the position code disabled/read-only and omit it from the update payload. Keep it editable and required only in create mode.

- [ ] Before changing department or level, request preview impact. If impact is nonzero, use `notification.confirm` with counts and explain that invalid manager/subordinate links will be cleared. Cancel leaves the dialog and data unchanged; confirm resends the update with `resetInvalidRelations: true`.

- [ ] If the transactional update returns `HIERARCHY_CONFIRMATION_REQUIRED` because data changed after preview, show the same system confirmation from the response counts and retry once with the flag.

- [ ] Replace all inline notices and `window.confirm` calls in the touched position flow with `useSystemNotification()` success/error/confirm APIs.

- [ ] Extend the department detail “Thêm vị trí” row with level and salary inputs and submit through `toCreatePositionPayload`, so positions created from either page obey identical rules and unique-code handling.

- [ ] Manually verify duplicate position code errors use the system notification component and say “Mã vị trí đã tồn tại”.

- [ ] Run lint and build.

```powershell
cd frontend/react-app
npm run lint
npm run build
```

- [ ] Commit only Task 8 paths.

---

## Task 9: Upgrade employee edit and subordinate-management UI

**Files:**

- Modify: `frontend/react-app/src/pages/admin/EmployeeManagement.jsx`
- Modify: `frontend/react-app/src/components/admin/Empolyee/EmployeeDialog.jsx`
- Modify: `frontend/react-app/src/hooks/adminManagementApi.js`
- Test: `frontend/react-app/src/pages/admin/positionManagementLogic.test.js`

- [ ] Display each position option as `CODE · Name · Cấp N · min – max` using the shared salary helper.

- [ ] Before submitting an edit whose `departmentId` or `positionId` changed, call employee hierarchy impact. Confirm nonzero impact via `SystemNotification`, set `resetInvalidRelations: true`, and handle a raced structured 409 with one confirmation/retry path.

- [ ] After a confirmed demotion or move, reload the employee list, selected employee detail, positions, and subordinate data so cleared relationships are visible immediately.

- [ ] Replace employee-page inline success/error banners, employee soft-delete `window.confirm`, subordinate add/remove `window.confirm`, and subordinate inline messages with `useSystemNotification()`.

- [ ] Replace the free-form subordinate email field with a select populated by `employeeApi.eligibleSubordinates(employee.id)`. Show employee code, full name, position, and level; submit the selected employee's email through the existing assignment endpoint. Refresh both eligible candidates and current subordinates after add/remove. The backend remains authoritative and repeats every rule on assignment.

- [ ] Ensure employees who have soft-deleted/resigned remain soft-deletable through the existing employee delete endpoint; this hierarchy change must not introduce a hard-delete path.

- [ ] Run frontend tests, lint, and build.

```powershell
cd frontend/react-app
node --test src/pages/admin/positionManagementLogic.test.js
npm run lint
npm run build
```

- [ ] Commit only Task 9 paths.

---

## Task 10: Full regression, API behavior, and browser verification

**Files:**

- Modify tests only if a genuine uncovered regression is found; do not weaken assertions.

- [ ] Run the full backend suite and record the exact test count and failures.

```powershell
cd backend/fashion-system
mvn test -q
```

- [ ] Run the full frontend verification.

```powershell
cd frontend/react-app
node --test src/pages/admin/*.test.js
npm run lint
npm run build
```

- [ ] Start the application with its normal project commands and verify in the browser:

  1. Create a position using a lowercase/spaced code and confirm it is stored uppercase.
  2. Attempt the same code in another department and confirm HTTP 409 is shown by `SystemNotification`.
  3. Edit a position and confirm its code cannot be changed and is absent from the PUT body.
  4. Confirm level and salary validation rejects zero level, negative salary, and maximum below minimum.
  5. Assign a higher-level full-time manager to a lower-level employee in the same department.
  6. Confirm equal-level and cross-department assignments are rejected.
  7. Demote an employee and cancel the warning; verify no data changes.
  8. Repeat and confirm; verify only newly invalid `managerId` links are cleared.
  9. Lower a position level with several reporting lines; verify preview counts, confirmation, deduplication, and refresh.
  10. Verify all confirmations, successes, and errors in touched flows use the system notification component.

- [ ] Verify Redis behavior with cache enabled: first position detail read populates `reference.position.detail`, update refreshes it, delete evicts it, and hierarchy previews reflect current DB state regardless of cache contents.

- [ ] Review `git diff --check`, inspect only intended files, and confirm the user’s pre-existing staged changes remain intact.

```powershell
git diff --check
git status --short
```

- [ ] If regression fixes were necessary, commit only their explicit paths. Otherwise do not create an empty final commit.
